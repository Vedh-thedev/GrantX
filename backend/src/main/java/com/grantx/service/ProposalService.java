package com.grantx.service;

import com.grantx.dto.ProposalDto;
import com.grantx.entity.*;
import com.grantx.exception.*;
import com.grantx.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final ReviewRepository reviewRepository;
    private final GrantFundingRepository grantFundingRepository;
    private final MilestoneRepository milestoneRepository;
    private final NotificationService notificationService;

    private static final Map<Proposal.ProposalStatus, Set<Proposal.ProposalStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.DRAFT,
                Set.of(Proposal.ProposalStatus.SUBMITTED));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.SUBMITTED,
                Set.of(Proposal.ProposalStatus.PENDING_EVALUATOR_ASSIGNMENT));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.PENDING_EVALUATOR_ASSIGNMENT,
                Set.of(Proposal.ProposalStatus.EVALUATORS_ASSIGNED));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.EVALUATORS_ASSIGNED,
                Set.of(Proposal.ProposalStatus.UNDER_REVIEW));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.UNDER_REVIEW,
                Set.of(Proposal.ProposalStatus.REVIEW_1_COMPLETED));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.REVIEW_1_COMPLETED,
                Set.of(Proposal.ProposalStatus.REVIEW_2_COMPLETED));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.REVIEW_2_COMPLETED,
                Set.of(Proposal.ProposalStatus.SCORES_AGGREGATED, Proposal.ProposalStatus.PENDING_GRANT_DECISION));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.SCORES_AGGREGATED,
                Set.of(Proposal.ProposalStatus.PENDING_GRANT_DECISION));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.PENDING_GRANT_DECISION,
                Set.of(Proposal.ProposalStatus.APPROVED, Proposal.ProposalStatus.REJECTED, Proposal.ProposalStatus.PENDING_REVISION));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.PENDING_REVISION,
                Set.of(Proposal.ProposalStatus.RESUBMITTED));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.RESUBMITTED,
                Set.of(Proposal.ProposalStatus.UNDER_REVIEW, Proposal.ProposalStatus.PENDING_EVALUATOR_ASSIGNMENT));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.APPROVED,
                Set.of(Proposal.ProposalStatus.GRANT_SANCTIONED));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.GRANT_SANCTIONED,
                Set.of(Proposal.ProposalStatus.MILESTONE_TRACKING));
        VALID_TRANSITIONS.put(Proposal.ProposalStatus.MILESTONE_TRACKING,
                Set.of(Proposal.ProposalStatus.COMPLETED));
    }

    @Transactional
    public ProposalDto.ProposalResponse createProposal(ProposalDto.CreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        Proposal.ProposalStatus initialStatus = request.isSaveDraft()
                ? Proposal.ProposalStatus.DRAFT
                : Proposal.ProposalStatus.SUBMITTED;

        String proposalNumber = generateProposalNumber();

        Proposal proposal = Proposal.builder()
                .proposalNumber(proposalNumber)
                .student(student)
                .title(request.getTitle())
                .problemStatement(request.getProblemStatement())
                .proposedSolution(request.getProposedSolution())
                .abstrakt(request.getAbstrakt())
                .objectives(request.getObjectives())
                .technologyDomain(request.getTechnologyDomain())
                .innovationCategory(request.getInnovationCategory())
                .existingSolutions(request.getExistingSolutions())
                .noveltyExplanation(request.getNoveltyExplanation())
                .differenceFromExisting(request.getDifferenceFromExisting())
                .innovationSignificance(request.getInnovationSignificance())
                .patentPotential(Proposal.PatentPotential.valueOf(
                        request.getPatentPotential() != null ? request.getPatentPotential() : "MEDIUM"))
                .noveltyDeclaration(request.getNoveltyDeclaration())
                .status(initialStatus)
                .build();

        if (initialStatus == Proposal.ProposalStatus.SUBMITTED) {
            proposal.setSubmissionDate(LocalDateTime.now());
        }

        proposal = proposalRepository.save(proposal);

        // Save team members
        if (request.getTeamMembers() != null && !request.getTeamMembers().isEmpty()) {
            List<ProposalTeamMember> members = new ArrayList<>();
            for (ProposalDto.TeamMemberDto m : request.getTeamMembers()) {
                members.add(ProposalTeamMember.builder()
                        .proposal(proposal)
                        .memberName(m.getMemberName())
                        .registerNumber(m.getRegisterNumber())
                        .department(m.getDepartment())
                        .yearOfStudy(m.getYearOfStudy())
                        .email(m.getEmail())
                        .build());
            }
            proposal.setTeamMembers(members);
        }

        // Save budget
        if (request.getBudget() != null) {
            ProposalDto.BudgetDto b = request.getBudget();
            ProposalBudget budget = ProposalBudget.builder()
                    .proposal(proposal)
                    .equipmentCost(b.getEquipmentCost() != null ? b.getEquipmentCost() : BigDecimal.ZERO)
                    .softwareCost(b.getSoftwareCost() != null ? b.getSoftwareCost() : BigDecimal.ZERO)
                    .materialsCost(b.getMaterialsCost() != null ? b.getMaterialsCost() : BigDecimal.ZERO)
                    .prototypeCost(b.getPrototypeCost() != null ? b.getPrototypeCost() : BigDecimal.ZERO)
                    .testingCost(b.getTestingCost() != null ? b.getTestingCost() : BigDecimal.ZERO)
                    .otherCost(b.getOtherCost() != null ? b.getOtherCost() : BigDecimal.ZERO)
                    .budgetJustification(b.getBudgetJustification())
                    .build();
            budget.calculateTotal();
            proposal.setBudget(budget);
        }

        proposal = proposalRepository.save(proposal);

        // Record status history
        recordStatusChange(proposal, null, initialStatus.name(), user, "Proposal created");

        // Notify
        if (initialStatus == Proposal.ProposalStatus.SUBMITTED) {
            notificationService.createNotification(user, proposal,
                    "Proposal Submitted",
                    "Your proposal '" + proposal.getTitle() + "' has been submitted successfully. Proposal number: " + proposalNumber,
                    "PROPOSAL_SUBMITTED");
        }

        return mapToResponse(proposal);
    }

    @Transactional
    public ProposalDto.ProposalResponse submitDraft(Long proposalId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));

        if (!proposal.getStudent().getId().equals(student.getId())) {
            throw new UnauthorizedException("You can only submit your own proposals");
        }
        if (proposal.getStatus() != Proposal.ProposalStatus.DRAFT) {
            throw new BusinessRuleException("Only draft proposals can be submitted");
        }

        proposal.setStatus(Proposal.ProposalStatus.SUBMITTED);
        proposal.setSubmissionDate(LocalDateTime.now());
        proposal = proposalRepository.save(proposal);

        recordStatusChange(proposal, "DRAFT", "SUBMITTED", user, "Draft submitted");
        notificationService.createNotification(user, proposal,
                "Proposal Submitted",
                "Your proposal '" + proposal.getTitle() + "' has been submitted for review.",
                "PROPOSAL_SUBMITTED");

        return mapToResponse(proposal);
    }

    public List<ProposalDto.ProposalResponse> getStudentProposals(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        return proposalRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProposalDto.ProposalDetailResponse getProposalDetail(Long proposalId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));

        // Authorization check
        if (user.getRole() == User.Role.STUDENT) {
            Student student = studentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
            if (!proposal.getStudent().getId().equals(student.getId())) {
                throw new UnauthorizedException("You can only view your own proposals");
            }
        }

        return mapToDetailResponse(proposal);
    }

    public List<ProposalDto.ProposalResponse> getAllProposals() {
        return proposalRepository.findAllSubmittedProposals().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProposalDto.ProposalResponse updateStatus(Long proposalId, String newStatus, String username, String remarks) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));

        Proposal.ProposalStatus targetStatus;
        try {
            targetStatus = Proposal.ProposalStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid status: " + newStatus);
        }

        validateStatusTransition(proposal.getStatus(), targetStatus);

        String prevStatus = proposal.getStatus().name();
        proposal.setStatus(targetStatus);
        proposal = proposalRepository.save(proposal);
        recordStatusChange(proposal, prevStatus, newStatus, user, remarks);

        return mapToResponse(proposal);
    }

    private void validateStatusTransition(Proposal.ProposalStatus current, Proposal.ProposalStatus target) {
        Set<Proposal.ProposalStatus> allowed = VALID_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new BusinessRuleException("Invalid status transition from " + current + " to " + target);
        }
    }

    private void recordStatusChange(Proposal proposal, String prev, String next, User user, String remarks) {
        statusHistoryRepository.save(StatusHistory.builder()
                .proposal(proposal)
                .previousStatus(prev)
                .newStatus(next)
                .changedBy(user)
                .remarks(remarks)
                .build());
    }

    private String generateProposalNumber() {
        int year = LocalDateTime.now().getYear();
        long count = proposalRepository.count() + 1;
        return String.format("GX-%d-%03d", year, count);
    }

    public Map<String, Object> getStudentDashboardStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        Map<String, Object> stats = new HashMap<>();
        List<Proposal> proposals = proposalRepository.findByStudent(student);
        stats.put("totalProposals", proposals.size());
        stats.put("draftProposals", proposals.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.DRAFT).count());
        stats.put("submittedProposals", proposals.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.SUBMITTED).count());
        stats.put("underReview", proposals.stream().filter(p -> isUnderReview(p.getStatus())).count());
        stats.put("approved", proposals.stream().filter(p -> isApproved(p.getStatus())).count());
        stats.put("rejected", proposals.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.REJECTED).count());

        BigDecimal totalFundingRequested = proposals.stream()
                .filter(p -> p.getBudget() != null)
                .map(p -> p.getBudget().getTotalRequested())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalFundingRequested", totalFundingRequested);

        BigDecimal totalFundingApproved = BigDecimal.valueOf(proposals.stream()
                .filter(p -> p.getStatus() == Proposal.ProposalStatus.APPROVED ||
                        p.getStatus() == Proposal.ProposalStatus.GRANT_SANCTIONED ||
                        p.getStatus() == Proposal.ProposalStatus.MILESTONE_TRACKING ||
                        p.getStatus() == Proposal.ProposalStatus.COMPLETED)
                .mapToLong(p -> {
                    Optional<GrantFunding> f = grantFundingRepository.findByProposal(p);
                    return f.map(gf -> gf.getApprovedAmount() != null ? gf.getApprovedAmount().longValue() : 0L).orElse(0L);
                })
                .boxed()
                .reduce(0L, Long::sum));
        stats.put("totalFundingApproved", totalFundingApproved);

        return stats;
    }

    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Proposal> all = proposalRepository.findAll();
        List<Proposal> submitted = all.stream().filter(p -> p.getStatus() != Proposal.ProposalStatus.DRAFT).collect(Collectors.toList());

        stats.put("totalProposals", submitted.size());
        stats.put("newSubmissions", submitted.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.SUBMITTED).count());
        stats.put("awaitingAssignment", submitted.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.PENDING_EVALUATOR_ASSIGNMENT).count());
        stats.put("evaluatorsAssigned", submitted.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.EVALUATORS_ASSIGNED || p.getStatus() == Proposal.ProposalStatus.UNDER_REVIEW).count());
        stats.put("underReview", submitted.stream().filter(p -> isUnderReview(p.getStatus())).count());
        stats.put("reviewsCompleted", submitted.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.REVIEW_2_COMPLETED || p.getStatus() == Proposal.ProposalStatus.SCORES_AGGREGATED || p.getStatus() == Proposal.ProposalStatus.PENDING_GRANT_DECISION).count());
        stats.put("pendingDecisions", submitted.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.PENDING_GRANT_DECISION).count());
        stats.put("approvedGrants", submitted.stream().filter(p -> isApproved(p.getStatus())).count());
        stats.put("rejected", submitted.stream().filter(p -> p.getStatus() == Proposal.ProposalStatus.REJECTED).count());
        stats.put("totalFundingRequested", grantFundingRepository.sumTotalRequested());
        stats.put("totalFundingApproved", grantFundingRepository.sumTotalApproved());

        return stats;
    }

    private boolean isUnderReview(Proposal.ProposalStatus s) {
        return s == Proposal.ProposalStatus.EVALUATORS_ASSIGNED ||
                s == Proposal.ProposalStatus.UNDER_REVIEW ||
                s == Proposal.ProposalStatus.REVIEW_1_COMPLETED ||
                s == Proposal.ProposalStatus.REVIEW_2_COMPLETED ||
                s == Proposal.ProposalStatus.SCORES_AGGREGATED;
    }

    private boolean isApproved(Proposal.ProposalStatus s) {
        return s == Proposal.ProposalStatus.APPROVED ||
                s == Proposal.ProposalStatus.GRANT_SANCTIONED ||
                s == Proposal.ProposalStatus.MILESTONE_TRACKING ||
                s == Proposal.ProposalStatus.COMPLETED;
    }

    public ProposalDto.ProposalResponse mapToResponse(Proposal p) {
        ProposalDto.ProposalResponse r = new ProposalDto.ProposalResponse();
        r.setId(p.getId());
        r.setProposalNumber(p.getProposalNumber());
        r.setTitle(p.getTitle());
        r.setStatus(p.getStatus().name());
        r.setInnovationCategory(p.getInnovationCategory());
        r.setTechnologyDomain(p.getTechnologyDomain());
        r.setPatentPotential(p.getPatentPotential().name());
        r.setStudentName(p.getStudent().getUser().getFullName());
        r.setStudentDepartment(p.getStudent().getDepartment());
        r.setStudentRegisterNumber(p.getStudent().getRegisterNumber());
        r.setAggregatedScore(p.getAggregatedScore());
        r.setTotalBudgetRequested(p.getBudget() != null ? p.getBudget().getTotalRequested() : BigDecimal.ZERO);
        r.setSubmissionDate(p.getSubmissionDate() != null ? p.getSubmissionDate().toString() : null);
        r.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        r.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        r.setReviewCount((int) reviewAssignmentRepository.countByProposal(p));
        r.setTeamMemberCount(p.getTeamMembers() != null ? p.getTeamMembers().size() : 0);
        return r;
    }

    public ProposalDto.ProposalDetailResponse mapToDetailResponse(Proposal p) {
        ProposalDto.ProposalDetailResponse r = new ProposalDto.ProposalDetailResponse();
        // Base fields
        r.setId(p.getId());
        r.setProposalNumber(p.getProposalNumber());
        r.setTitle(p.getTitle());
        r.setStatus(p.getStatus().name());
        r.setInnovationCategory(p.getInnovationCategory());
        r.setTechnologyDomain(p.getTechnologyDomain());
        r.setPatentPotential(p.getPatentPotential().name());
        r.setStudentName(p.getStudent().getUser().getFullName());
        r.setStudentDepartment(p.getStudent().getDepartment());
        r.setStudentRegisterNumber(p.getStudent().getRegisterNumber());
        r.setAggregatedScore(p.getAggregatedScore());
        r.setTotalBudgetRequested(p.getBudget() != null ? p.getBudget().getTotalRequested() : BigDecimal.ZERO);
        r.setSubmissionDate(p.getSubmissionDate() != null ? p.getSubmissionDate().toString() : null);
        r.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        r.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        r.setReviewCount((int) reviewAssignmentRepository.countByProposal(p));
        r.setTeamMemberCount(p.getTeamMembers() != null ? p.getTeamMembers().size() : 0);
        // Detail fields
        r.setProblemStatement(p.getProblemStatement());
        r.setProposedSolution(p.getProposedSolution());
        r.setAbstrakt(p.getAbstrakt());
        r.setObjectives(p.getObjectives());
        r.setExistingSolutions(p.getExistingSolutions());
        r.setNoveltyExplanation(p.getNoveltyExplanation());
        r.setDifferenceFromExisting(p.getDifferenceFromExisting());
        r.setInnovationSignificance(p.getInnovationSignificance());
        r.setNoveltyDeclaration(p.getNoveltyDeclaration());

        // Team members
        if (p.getTeamMembers() != null) {
            r.setTeamMembers(p.getTeamMembers().stream().map(m -> {
                ProposalDto.TeamMemberDto tm = new ProposalDto.TeamMemberDto();
                tm.setMemberName(m.getMemberName());
                tm.setRegisterNumber(m.getRegisterNumber());
                tm.setDepartment(m.getDepartment());
                tm.setYearOfStudy(m.getYearOfStudy());
                tm.setEmail(m.getEmail());
                return tm;
            }).collect(Collectors.toList()));
        }

        // Budget
        if (p.getBudget() != null) {
            ProposalBudget b = p.getBudget();
            ProposalDto.BudgetDetailDto bd = new ProposalDto.BudgetDetailDto();
            bd.setEquipmentCost(b.getEquipmentCost());
            bd.setSoftwareCost(b.getSoftwareCost());
            bd.setMaterialsCost(b.getMaterialsCost());
            bd.setPrototypeCost(b.getPrototypeCost());
            bd.setTestingCost(b.getTestingCost());
            bd.setOtherCost(b.getOtherCost());
            bd.setTotalRequested(b.getTotalRequested());
            bd.setBudgetJustification(b.getBudgetJustification());
            r.setBudget(bd);
        }

        // Status history
        List<StatusHistory> history = statusHistoryRepository.findByProposalOrderByChangedAtAsc(p);
        r.setStatusHistory(history.stream().map(h -> {
            ProposalDto.StatusHistoryDto sh = new ProposalDto.StatusHistoryDto();
            sh.setPreviousStatus(h.getPreviousStatus());
            sh.setNewStatus(h.getNewStatus());
            sh.setChangedBy(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System");
            sh.setRemarks(h.getRemarks());
            sh.setChangedAt(h.getChangedAt() != null ? h.getChangedAt().toString() : null);
            return sh;
        }).collect(Collectors.toList()));

        // Milestones
        List<Milestone> milestones = milestoneRepository.findByProposalOrderByDueDateAsc(p);
        r.setMilestones(milestones.stream().map(m -> {
            ProposalDto.MilestoneDto md = new ProposalDto.MilestoneDto();
            md.setId(m.getId());
            md.setTitle(m.getTitle());
            md.setDescription(m.getDescription());
            md.setDueDate(m.getDueDate() != null ? m.getDueDate().toString() : null);
            md.setStatus(m.getStatus().name());
            md.setCompletionPercentage(m.getCompletionPercentage());
            md.setRemarks(m.getRemarks());
            md.setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
            md.setUpdatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
            return md;
        }).collect(Collectors.toList()));

        // Funding
        grantFundingRepository.findByProposal(p).ifPresent(f -> {
            ProposalDto.GrantFundingDto fd = new ProposalDto.GrantFundingDto();
            fd.setId(f.getId());
            fd.setRequestedAmount(f.getRequestedAmount());
            fd.setApprovedAmount(f.getApprovedAmount());
            fd.setGrantStatus(f.getGrantStatus().name());
            fd.setSanctionDate(f.getSanctionDate() != null ? f.getSanctionDate().toString() : null);
            fd.setGrantReference(f.getGrantReference());
            fd.setRemarks(f.getRemarks());
            fd.setProjectPhase(f.getProjectPhase());
            r.setFunding(fd);
        });

        return r;
    }
}
