package com.grantx.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public class ReviewDto {

    @Data
    public static class AssignEvaluatorsRequest {
        @NotNull(message = "Proposal ID is required")
        private Long proposalId;

        @NotNull(message = "Evaluator 1 ID is required")
        private Long evaluator1FacultyId;

        @NotNull(message = "Evaluator 2 ID is required")
        private Long evaluator2FacultyId;

        private String deadline;
    }

    @Data
    public static class SubmitReviewRequest {
        @NotNull(message = "Assignment ID is required")
        private Long assignmentId;

        @NotNull(message = "Novelty score is required")
        @DecimalMin(value = "0.0", message = "Novelty score must be at least 0")
        @DecimalMax(value = "10.0", message = "Novelty score must be at most 10")
        private BigDecimal noveltyScore;

        @NotNull(message = "Feasibility score is required")
        @DecimalMin(value = "0.0", message = "Feasibility score must be at least 0")
        @DecimalMax(value = "10.0", message = "Feasibility score must be at most 10")
        private BigDecimal feasibilityScore;

        @NotNull(message = "Commercial impact score is required")
        @DecimalMin(value = "0.0", message = "Commercial impact score must be at least 0")
        @DecimalMax(value = "10.0", message = "Commercial impact score must be at most 10")
        private BigDecimal commercialImpactScore;

        @NotBlank(message = "Technical comments are required")
        private String technicalComments;

        @NotBlank(message = "Strengths are required")
        private String strengths;

        @NotBlank(message = "Weaknesses are required")
        private String weaknesses;

        @NotBlank(message = "Recommendation is required")
        private String recommendation;

        private String additionalRemarks;
    }

    @Data
    public static class ReviewResponse {
        private Long id;
        private Long assignmentId;
        private Long proposalId;
        private String proposalTitle;
        private String proposalNumber;
        private Long facultyId;
        private String facultyName;
        private Integer evaluatorNumber;
        private String technicalComments;
        private String strengths;
        private String weaknesses;
        private String recommendation;
        private String additionalRemarks;
        private BigDecimal weightedScore;
        private boolean isLocked;
        private String submittedAt;
        private ReviewScoreDto scores;
        private String assignmentStatus;
        private String deadline;
    }

    @Data
    public static class ReviewScoreDto {
        private BigDecimal noveltyScore;
        private BigDecimal feasibilityScore;
        private BigDecimal commercialImpactScore;
        private BigDecimal weightedTotal;
    }

    @Data
    public static class AssignmentResponse {
        private Long id;
        private Long proposalId;
        private String proposalTitle;
        private String proposalNumber;
        private Long facultyId;
        private String facultyName;
        private String facultyDepartment;
        private Integer evaluatorNumber;
        private String status;
        private String deadline;
        private String assignedAt;
        private boolean hasReview;
        private BigDecimal reviewScore;
    }

    @Data
    public static class ReviewMonitorResponse {
        private Long proposalId;
        private String proposalTitle;
        private String proposalNumber;
        private String studentName;
        private String department;
        private String proposalStatus;
        private EvaluatorStatusDto evaluator1;
        private EvaluatorStatusDto evaluator2;
        private BigDecimal aggregatedScore;
    }

    @Data
    public static class EvaluatorStatusDto {
        private Long facultyId;
        private String facultyName;
        private String status;
        private BigDecimal score;
        private String submittedAt;
    }
}
