package com.grantx.service;

import com.grantx.dto.ReviewDto;
import com.grantx.entity.*;
import com.grantx.exception.*;
import com.grantx.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewScoreRepository reviewScoreRepository;
    private final ProposalRepository proposalRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final NotificationService notificationService;

    @Transactional
    public void assignEvaluators(ReviewDto.AssignEvaluatorsRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Proposal proposal = proposalRepository.findById(request.getProposalId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", request.getProposalId()));

        // Validate proposal can be assigned
        if (proposal.getStatus() != Proposal.ProposalStatus.SUBMITTED &&
                proposal.getStatus() != Proposal.ProposalStatus.PENDING_EVALUATOR_ASSIGNMENT) {
            throw new BusinessRuleException("Proposal is not in a state where evaluators can be assigned. Current status: " + proposal.getStatus());
        }

        Faculty eval1 = facultyRepository.findById(request.getEvaluator1FacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty 1 not found"));
        Faculty eval2 = facultyRepository.findById(request.getEvaluator2FacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty 2 not found"));

        // Business rule: evaluators must be different
        if (eval1.getId().equals(eval2.getId())) {
            throw new BusinessRuleException("Evaluator 1 and Evaluator 2 must be different faculty members");
        }

        // Check for existing assignments
        if (reviewAssignmentRepository.existsByProposalAndFaculty(proposal, eval1)) {
            throw new BusinessRuleException("Faculty member already assigned to this proposal: " + eval1.getUser().getFullName());
        }
        if (reviewAssignmentRepository.existsByProposalAndFaculty(proposal, eval2)) {
            throw new BusinessRuleException("Faculty member already assigned to this proposal: " + eval2.getUser().getFullName());
        }

        LocalDate deadline = request.getDeadline() != null
                ? LocalDate.parse(request.getDeadline())
                : LocalDate.now().plusDays(14);

        ReviewAssignment assignment1 = ReviewAssignment.builder()
                .proposal(proposal)
                .faculty(eval1)
                .evaluatorNumber(1)
                .assignedBy(admin)
                .deadline(deadline)
                .status(ReviewAssignment.AssignmentStatus.PENDING)
                .build();
        reviewAssignmentRepository.save(assignment1);

        ReviewAssignment assignment2 = ReviewAssignment.builder()
                .proposal(proposal)
                .faculty(eval2)
                .evaluatorNumber(2)
                .assignedBy(admin)
                .deadline(deadline)
                .status(ReviewAssignment.AssignmentStatus.PENDING)
                .build();
        reviewAssignmentRepository.save(assignment2);

        // Update proposal status
        String prevStatus = proposal.getStatus().name();
        proposal.setStatus(Proposal.ProposalStatus.EVALUATORS_ASSIGNED);
        proposalRepository.save(proposal);

        statusHistoryRepository.save(StatusHistory.builder()
                .proposal(proposal)
                .previousStatus(prevStatus)
                .newStatus("EVALUATORS_ASSIGNED")
                .changedBy(admin)
                .remarks("Assigned: " + eval1.getUser().getFullName() + " (E1), " + eval2.getUser().getFullName() + " (E2)")
                .build());

        // Notify evaluators
        notificationService.createNotification(eval1.getUser(), proposal,
                "New Proposal Assigned for Evaluation",
                "You have been assigned as Evaluator 1 for proposal: '" + proposal.getTitle() + "' (" + proposal.getProposalNumber() + "). Deadline: " + deadline,
                "ASSIGNMENT");
        notificationService.createNotification(eval2.getUser(), proposal,
                "New Proposal Assigned for Evaluation",
                "You have been assigned as Evaluator 2 for proposal: '" + proposal.getTitle() + "' (" + proposal.getProposalNumber() + "). Deadline: " + deadline,
                "ASSIGNMENT");

        // Notify student
        notificationService.createNotification(proposal.getStudent().getUser(), proposal,
                "Evaluators Assigned",
                "Two faculty evaluators have been assigned to review your proposal '" + proposal.getTitle() + "'. Evaluation is now in progress.",
                "EVALUATORS_ASSIGNED");
    }

    @Transactional
    public ReviewDto.ReviewResponse submitReview(ReviewDto.SubmitReviewRequest request, String facultyUsername) {
        User facultyUser = userRepository.findByUsername(facultyUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Faculty faculty = facultyRepository.findByUserId(facultyUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty profile not found"));

        ReviewAssignment assignment = reviewAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", request.getAssignmentId()));

        // Authorization: faculty must be assigned to this
        if (!assignment.getFaculty().getId().equals(faculty.getId())) {
            throw new UnauthorizedException("You are not authorized to review this proposal");
        }

        // Check for duplicate review
        if (reviewRepository.existsByAssignmentId(assignment.getId())) {
            throw new BusinessRuleException("You have already submitted a review for this proposal. Reviews are locked.");
        }

        // Validate proposal status
        Proposal proposal = assignment.getProposal();

        Review.Recommendation recommendation;
        try {
            recommendation = Review.Recommendation.valueOf(request.getRecommendation());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid recommendation: " + request.getRecommendation());
        }

        Review review = Review.builder()
                .assignment(assignment)
                .proposal(proposal)
                .faculty(faculty)
                .technicalComments(request.getTechnicalComments())
                .strengths(request.getStrengths())
                .weaknesses(request.getWeaknesses())
                .recommendation(recommendation)
                .additionalRemarks(request.getAdditionalRemarks())
                .isLocked(true)
                .build();

        review = reviewRepository.save(review);

        // Calculate and save scores
        ReviewScore score = ReviewScore.builder()
                .review(review)
                .noveltyScore(request.getNoveltyScore())
                .feasibilityScore(request.getFeasibilityScore())
                .commercialImpactScore(request.getCommercialImpactScore())
                .weightedTotal(BigDecimal.ZERO)
                .build();
        score.calculateWeightedTotal();
        score = reviewScoreRepository.save(score);

        review.setWeightedScore(score.getWeightedTotal());
        review.setReviewScore(score);
        reviewRepository.save(review);

        // Update assignment status
        assignment.setStatus(ReviewAssignment.AssignmentStatus.COMPLETED);
        reviewAssignmentRepository.save(assignment);

        // Update proposal status and check aggregation
        updateProposalAfterReview(proposal, assignment.getEvaluatorNumber(), facultyUser);

        return mapReviewToResponse(review, assignment);
    }

    private void updateProposalAfterReview(Proposal proposal, int evaluatorNumber, User changedBy) {
        List<ReviewAssignment> assignments = reviewAssignmentRepository.findByProposalOrderByEvaluatorNumberAsc(proposal);
        long completedCount = assignments.stream()
                .filter(a -> a.getStatus() == ReviewAssignment.AssignmentStatus.COMPLETED)
                .count();

        String prevStatus = proposal.getStatus().name();
        if (completedCount == 1) {
            proposal.setStatus(Proposal.ProposalStatus.REVIEW_1_COMPLETED);
            proposalRepository.save(proposal);
            statusHistoryRepository.save(StatusHistory.builder()
                    .proposal(proposal).previousStatus(prevStatus).newStatus("REVIEW_1_COMPLETED")
                    .changedBy(changedBy).remarks("Evaluator " + evaluatorNumber + " submitted review").build());
        } else if (completedCount == 2) {
            // Aggregate scores
            List<Review> reviews = reviewRepository.findByProposal(proposal);
            if (reviews.size() == 2) {
                BigDecimal score1 = reviews.get(0).getWeightedScore();
                BigDecimal score2 = reviews.get(1).getWeightedScore();
                BigDecimal aggregated = score1.add(score2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                proposal.setAggregatedScore(aggregated);
            }
            proposal.setStatus(Proposal.ProposalStatus.PENDING_GRANT_DECISION);
            proposalRepository.save(proposal);
            statusHistoryRepository.save(StatusHistory.builder()
                    .proposal(proposal).previousStatus(prevStatus).newStatus("PENDING_GRANT_DECISION")
                    .changedBy(changedBy).remarks("Both reviews completed. Scores aggregated. Ready for R&D decision.").build());

            // Notify R&D (admin users)
            userRepository.findByRole(User.Role.ADMIN).forEach(admin ->
                    notificationService.createNotification(admin, proposal,
                            "Proposal Ready for Decision",
                            "Both evaluations completed for '" + proposal.getTitle() + "'. Aggregated score: " + proposal.getAggregatedScore() + "/10. Ready for R&D decision.",
                            "REVIEWS_COMPLETE"));

            // Notify student
            notificationService.createNotification(proposal.getStudent().getUser(), proposal,
                    "Evaluation Complete",
                    "Both faculty evaluations for your proposal '" + proposal.getTitle() + "' are complete. Awaiting R&D Committee decision.",
                    "EVALUATION_COMPLETE");
        }
    }

    public List<ReviewDto.AssignmentResponse> getFacultyAssignments(String facultyUsername) {
        User user = userRepository.findByUsername(facultyUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Faculty faculty = facultyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty profile not found"));
        return reviewAssignmentRepository.findByFaculty(faculty).stream()
                .map(a -> mapAssignmentToResponse(a, faculty))
                .collect(Collectors.toList());
    }

    public ReviewDto.ReviewResponse getReviewForAssignment(Long assignmentId, String facultyUsername) {
        User user = userRepository.findByUsername(facultyUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Faculty faculty = facultyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty profile not found"));
        ReviewAssignment assignment = reviewAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        if (!assignment.getFaculty().getId().equals(faculty.getId())) {
            throw new UnauthorizedException("Not authorized to access this assignment");
        }
        Review review = reviewRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not yet submitted for this assignment"));
        return mapReviewToResponse(review, assignment);
    }

    public List<ReviewDto.ReviewMonitorResponse> getAllReviewsForAdmin() {
        List<Proposal> proposals = proposalRepository.findAllSubmittedProposals();
        return proposals.stream()
                .filter(p -> p.getStatus() != Proposal.ProposalStatus.DRAFT &&
                        p.getStatus() != Proposal.ProposalStatus.SUBMITTED &&
                        p.getStatus() != Proposal.ProposalStatus.PENDING_EVALUATOR_ASSIGNMENT)
                .map(this::mapToMonitorResponse)
                .collect(Collectors.toList());
    }

    public List<ReviewDto.ReviewResponse> getReviewsForProposal(Long proposalId, String adminUsername) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));
        return reviewRepository.findByProposal(proposal).stream()
                .map(r -> {
                    ReviewAssignment a = r.getAssignment();
                    return mapReviewToResponse(r, a);
                })
                .collect(Collectors.toList());
    }

    private ReviewDto.ReviewMonitorResponse mapToMonitorResponse(Proposal p) {
        ReviewDto.ReviewMonitorResponse r = new ReviewDto.ReviewMonitorResponse();
        r.setProposalId(p.getId());
        r.setProposalTitle(p.getTitle());
        r.setProposalNumber(p.getProposalNumber());
        r.setStudentName(p.getStudent().getUser().getFullName());
        r.setDepartment(p.getStudent().getDepartment());
        r.setProposalStatus(p.getStatus().name());
        r.setAggregatedScore(p.getAggregatedScore());

        List<ReviewAssignment> assignments = reviewAssignmentRepository.findByProposalOrderByEvaluatorNumberAsc(p);
        for (ReviewAssignment a : assignments) {
            ReviewDto.EvaluatorStatusDto eval = new ReviewDto.EvaluatorStatusDto();
            eval.setFacultyId(a.getFaculty().getId());
            eval.setFacultyName(a.getFaculty().getUser().getFullName());
            eval.setStatus(a.getStatus().name());
            reviewRepository.findByAssignmentId(a.getId()).ifPresent(rev -> {
                eval.setScore(rev.getWeightedScore());
                eval.setSubmittedAt(rev.getSubmittedAt() != null ? rev.getSubmittedAt().toString() : null);
            });
            if (a.getEvaluatorNumber() == 1) r.setEvaluator1(eval);
            else r.setEvaluator2(eval);
        }
        return r;
    }

    private ReviewDto.AssignmentResponse mapAssignmentToResponse(ReviewAssignment a, Faculty faculty) {
        ReviewDto.AssignmentResponse r = new ReviewDto.AssignmentResponse();
        r.setId(a.getId());
        r.setProposalId(a.getProposal().getId());
        r.setProposalTitle(a.getProposal().getTitle());
        r.setProposalNumber(a.getProposal().getProposalNumber());
        r.setFacultyId(a.getFaculty().getId());
        r.setFacultyName(a.getFaculty().getUser().getFullName());
        r.setFacultyDepartment(a.getFaculty().getDepartment());
        r.setEvaluatorNumber(a.getEvaluatorNumber());
        r.setStatus(a.getStatus().name());
        r.setDeadline(a.getDeadline() != null ? a.getDeadline().toString() : null);
        r.setAssignedAt(a.getAssignedAt() != null ? a.getAssignedAt().toString() : null);
        r.setHasReview(reviewRepository.existsByAssignmentId(a.getId()));
        reviewRepository.findByAssignmentId(a.getId()).ifPresent(rev -> r.setReviewScore(rev.getWeightedScore()));
        return r;
    }

    private ReviewDto.ReviewResponse mapReviewToResponse(Review review, ReviewAssignment assignment) {
        ReviewDto.ReviewResponse r = new ReviewDto.ReviewResponse();
        r.setId(review.getId());
        r.setAssignmentId(assignment.getId());
        r.setProposalId(review.getProposal().getId());
        r.setProposalTitle(review.getProposal().getTitle());
        r.setProposalNumber(review.getProposal().getProposalNumber());
        r.setFacultyId(review.getFaculty().getId());
        r.setFacultyName(review.getFaculty().getUser().getFullName());
        r.setEvaluatorNumber(assignment.getEvaluatorNumber());
        r.setTechnicalComments(review.getTechnicalComments());
        r.setStrengths(review.getStrengths());
        r.setWeaknesses(review.getWeaknesses());
        r.setRecommendation(review.getRecommendation().name());
        r.setAdditionalRemarks(review.getAdditionalRemarks());
        r.setWeightedScore(review.getWeightedScore());
        r.setLocked(review.getIsLocked());
        r.setSubmittedAt(review.getSubmittedAt() != null ? review.getSubmittedAt().toString() : null);
        r.setAssignmentStatus(assignment.getStatus().name());
        r.setDeadline(assignment.getDeadline() != null ? assignment.getDeadline().toString() : null);
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
