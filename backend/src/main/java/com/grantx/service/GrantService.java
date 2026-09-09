package com.grantx.service;

import com.grantx.dto.GrantDto;
import com.grantx.dto.ProposalDto;
import com.grantx.dto.ReviewDto;
import com.grantx.entity.*;
import com.grantx.exception.*;
import com.grantx.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrantService {

    private final ProposalRepository proposalRepository;
    private final GrantDecisionRepository grantDecisionRepository;
    private final GrantFundingRepository grantFundingRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final ProposalService proposalService;

    @Transactional
    public GrantDecision makeDecision(GrantDto.DecisionRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Proposal proposal = proposalRepository.findById(request.getProposalId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", request.getProposalId()));

        // Ensure both reviews are done
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING_GRANT_DECISION &&
                proposal.getStatus() != Proposal.ProposalStatus.REVIEW_2_COMPLETED &&
                proposal.getStatus() != Proposal.ProposalStatus.SCORES_AGGREGATED) {
            throw new BusinessRuleException("Cannot make a decision. Proposal must be in PENDING_GRANT_DECISION state. Current: " + proposal.getStatus());
        }

        long completedReviews = reviewAssignmentRepository.findByProposal(proposal).stream()
                .filter(a -> a.getStatus() == ReviewAssignment.AssignmentStatus.COMPLETED)
                .count();
        if (completedReviews < 2) {
            throw new BusinessRuleException("Cannot make decision. Both evaluator reviews must be completed first. Completed: " + completedReviews + "/2");
        }

        GrantDecision.Decision decision;
        try {
            decision = GrantDecision.Decision.valueOf(request.getDecision().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid decision: " + request.getDecision());
        }

        GrantDecision grantDecision = GrantDecision.builder()
                .proposal(proposal)
                .decidedBy(admin)
                .decision(decision)
                .decisionRemarks(request.getDecisionRemarks())
                .revisionComments(request.getRevisionComments())
                .aggregatedScoreAtDecision(proposal.getAggregatedScore())
                .build();
        grantDecision = grantDecisionRepository.save(grantDecision);

        String prevStatus = proposal.getStatus().name();
        String notifTitle, notifMessage, notifType;

        if (decision == GrantDecision.Decision.APPROVED) {
            proposal.setStatus(Proposal.ProposalStatus.APPROVED);
            proposalRepository.save(proposal);
            statusHistoryRepository.save(StatusHistory.builder()
                    .proposal(proposal).previousStatus(prevStatus).newStatus("APPROVED")
                    .changedBy(admin).remarks("Approved: " + request.getDecisionRemarks()).build());

            // Create grant funding record
            BigDecimal requested = proposal.getBudget() != null ? proposal.getBudget().getTotalRequested() : BigDecimal.ZERO;
            BigDecimal approved = request.getApprovedAmount() != null ? request.getApprovedAmount() : requested;
            GrantFunding funding = GrantFunding.builder()
                    .proposal(proposal)
                    .decision(grantDecision)
                    .requestedAmount(requested)
                    .approvedAmount(approved)
                    .grantStatus(GrantFunding.GrantStatus.APPROVED)
                    .sanctionDate(request.getSanctionDate() != null ? LocalDate.parse(request.getSanctionDate()) : LocalDate.now())
                    .grantReference(request.getGrantReference())
                    .remarks(request.getDecisionRemarks())
                    .projectPhase(request.getProjectPhase() != null ? request.getProjectPhase() : "Phase 1: Initial Development")
                    .build();
            grantFundingRepository.save(funding);

            // Move to grant sanctioned
            prevStatus = "APPROVED";
            proposal.setStatus(Proposal.ProposalStatus.GRANT_SANCTIONED);
            proposalRepository.save(proposal);
            statusHistoryRepository.save(StatusHistory.builder()
                    .proposal(proposal).previousStatus(prevStatus).newStatus("GRANT_SANCTIONED")
                    .changedBy(admin).remarks("Grant sanctioned. Amount: Rs. " + approved).build());

            notifTitle = "Proposal Approved & Grant Sanctioned!";
            notifMessage = "Congratulations! Your proposal '" + proposal.getTitle() + "' has been APPROVED. Grant amount: Rs. " + approved + ". Reference: " + (request.getGrantReference() != null ? request.getGrantReference() : "N/A");
            notifType = "PROPOSAL_APPROVED";

        } else if (decision == GrantDecision.Decision.REJECTED) {
            proposal.setStatus(Proposal.ProposalStatus.REJECTED);
            proposalRepository.save(proposal);
            statusHistoryRepository.save(StatusHistory.builder()
                    .proposal(proposal).previousStatus(prevStatus).newStatus("REJECTED")
                    .changedBy(admin).remarks("Rejected: " + request.getDecisionRemarks()).build());

            notifTitle = "Proposal Not Selected";
            notifMessage = "Your proposal '" + proposal.getTitle() + "' was not selected for funding at this time. Remarks: " + request.getDecisionRemarks();
            notifType = "PROPOSAL_REJECTED";

        } else { // REVISION_REQUESTED
            proposal.setStatus(Proposal.ProposalStatus.PENDING_REVISION);
            proposalRepository.save(proposal);
            statusHistoryRepository.save(StatusHistory.builder()
                    .proposal(proposal).previousStatus(prevStatus).newStatus("PENDING_REVISION")
                    .changedBy(admin).remarks("Revision requested: " + request.getRevisionComments()).build());

            notifTitle = "Revision Requested";
            notifMessage = "The R&D Committee has requested revisions to your proposal '" + proposal.getTitle() + "'. Please review the comments and resubmit. Comments: " + request.getRevisionComments();
            notifType = "REVISION_REQUESTED";
        }

        notificationService.createNotification(proposal.getStudent().getUser(), proposal, notifTitle, notifMessage, notifType);

        return grantDecision;
    }

    @Transactional
    public Milestone createMilestone(GrantDto.CreateMilestoneRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Proposal proposal = proposalRepository.findById(request.getProposalId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", request.getProposalId()));

        if (proposal.getStatus() != Proposal.ProposalStatus.GRANT_SANCTIONED &&
                proposal.getStatus() != Proposal.ProposalStatus.MILESTONE_TRACKING &&
                proposal.getStatus() != Proposal.ProposalStatus.COMPLETED) {
            throw new BusinessRuleException("Milestones can only be created for sanctioned/active grants");
        }

        Milestone.MilestoneStatus status;
        try {
            status = Milestone.MilestoneStatus.valueOf(request.getStatus() != null ? request.getStatus() : "NOT_STARTED");
        } catch (IllegalArgumentException e) {
            status = Milestone.MilestoneStatus.NOT_STARTED;
        }

        Milestone milestone = Milestone.builder()
                .proposal(proposal)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(LocalDate.parse(request.getDueDate()))
                .status(status)
                .completionPercentage(request.getCompletionPercentage() != null ? request.getCompletionPercentage() : 0)
                .remarks(request.getRemarks())
                .createdBy(admin)
                .build();

        milestone = milestoneRepository.save(milestone);

        // Update proposal to MILESTONE_TRACKING if not already
        if (proposal.getStatus() == Proposal.ProposalStatus.GRANT_SANCTIONED) {
            proposal.setStatus(Proposal.ProposalStatus.MILESTONE_TRACKING);
            proposalRepository.save(proposal);
            statusHistoryRepository.save(StatusHistory.builder()
                    .proposal(proposal).previousStatus("GRANT_SANCTIONED").newStatus("MILESTONE_TRACKING")
                    .changedBy(admin).remarks("Milestone tracking started").build());
        }

        notificationService.createNotification(proposal.getStudent().getUser(), proposal,
                "New Project Milestone Added",
                "A new milestone '" + milestone.getTitle() + "' has been added to your project '" + proposal.getTitle() + "'. Due: " + request.getDueDate(),
                "MILESTONE_ADDED");

        return milestone;
    }

    @Transactional
    public Milestone updateMilestone(Long milestoneId, GrantDto.UpdateMilestoneRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        if (request.getStatus() != null) {
            milestone.setStatus(Milestone.MilestoneStatus.valueOf(request.getStatus()));
        }
        if (request.getCompletionPercentage() != null) {
            milestone.setCompletionPercentage(request.getCompletionPercentage());
        }
        if (request.getRemarks() != null) {
            milestone.setRemarks(request.getRemarks());
        }
        if (request.getDueDate() != null) {
            milestone.setDueDate(LocalDate.parse(request.getDueDate()));
        }

        milestone = milestoneRepository.save(milestone);

        notificationService.createNotification(milestone.getProposal().getStudent().getUser(), milestone.getProposal(),
                "Milestone Updated",
                "Milestone '" + milestone.getTitle() + "' has been updated. Status: " + milestone.getStatus() + ". Progress: " + milestone.getCompletionPercentage() + "%",
                "MILESTONE_UPDATED");

        return milestone;
    }

    public List<GrantFunding> getAllGrants() {
        return grantFundingRepository.findAll();
    }

    public List<Milestone> getMilestonesForProposal(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));
        return milestoneRepository.findByProposalOrderByDueDateAsc(proposal);
    }

    public GrantDto.DecisionSummaryResponse getDecisionSummary(Long proposalId, String adminUsername) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));

        GrantDto.DecisionSummaryResponse r = new GrantDto.DecisionSummaryResponse();
        r.setProposalId(proposal.getId());
        r.setProposalNumber(proposal.getProposalNumber());
        r.setProposalTitle(proposal.getTitle());
        r.setStudentName(proposal.getStudent().getUser().getFullName());
        r.setStudentDepartment(proposal.getStudent().getDepartment());
        r.setRequestedFunding(proposal.getBudget() != null ? proposal.getBudget().getTotalRequested() : BigDecimal.ZERO);
        r.setAggregatedScore(proposal.getAggregatedScore());
        r.setProposalStatus(proposal.getStatus().name());

        List<ReviewAssignment> assignments = reviewAssignmentRepository.findByProposalOrderByEvaluatorNumberAsc(proposal);
        for (ReviewAssignment a : assignments) {
            ReviewDto.EvaluatorStatusDto eval = new ReviewDto.EvaluatorStatusDto();
            eval.setFacultyId(a.getFaculty().getId());
            eval.setFacultyName(a.getFaculty().getUser().getFullName());
            eval.setStatus(a.getStatus().name());

            ReviewDto.ReviewResponse reviewDetail = null;
            var reviewOpt = reviewRepository.findByAssignmentId(a.getId());
            if (reviewOpt.isPresent()) {
                Review rev = reviewOpt.get();
                eval.setScore(rev.getWeightedScore());
                eval.setSubmittedAt(rev.getSubmittedAt() != null ? rev.getSubmittedAt().toString() : null);
                reviewDetail = mapReviewToDetailResponse(rev, a);
            }

            if (a.getEvaluatorNumber() == 1) {
                r.setEvaluator1(eval);
                r.setReview1Details(reviewDetail);
            } else {
                r.setEvaluator2(eval);
                r.setReview2Details(reviewDetail);
            }
        }

        grantDecisionRepository.findTopByProposalOrderByCreatedAtDesc(proposal).ifPresent(d -> {
            ProposalDto.GrantDecisionDto dd = new ProposalDto.GrantDecisionDto();
            dd.setId(d.getId());
            dd.setDecision(d.getDecision().name());
            dd.setDecisionRemarks(d.getDecisionRemarks());
            dd.setRevisionComments(d.getRevisionComments());
            dd.setAggregatedScoreAtDecision(d.getAggregatedScoreAtDecision());
            dd.setDecisionDate(d.getDecisionDate() != null ? d.getDecisionDate().toString() : null);
            dd.setDecidedBy(d.getDecidedBy().getFullName());
            r.setLatestDecision(dd);
        });

        return r;
    }

    private ReviewDto.ReviewResponse mapReviewToDetailResponse(Review review, ReviewAssignment assignment) {
        ReviewDto.ReviewResponse r = new ReviewDto.ReviewResponse();
        r.setId(review.getId());
        r.setFacultyName(review.getFaculty().getUser().getFullName());
        r.setEvaluatorNumber(assignment.getEvaluatorNumber());
        r.setTechnicalComments(review.getTechnicalComments());
        r.setStrengths(review.getStrengths());
        r.setWeaknesses(review.getWeaknesses());
        r.setRecommendation(review.getRecommendation().name());
        r.setAdditionalRemarks(review.getAdditionalRemarks());
        r.setWeightedScore(review.getWeightedScore());
        r.setSubmittedAt(review.getSubmittedAt() != null ? review.getSubmittedAt().toString() : null);
        if (review.getReviewScore() != null) {
            ReviewDto.ReviewScoreDto sd = new ReviewDto.ReviewScoreDto();
            sd.setNoveltyScore(review.getReviewScore().getNoveltyScore());
            sd.setFeasibilityScore(review.getReviewScore().getFeasibilityScore());
            sd.setCommercialImpactScore(review.getReviewScore().getCommercialImpactScore());
            sd.setWeightedTotal(review.getReviewScore().getWeightedTotal());
            r.setScores(sd);
        }
        return r;
    }
}
