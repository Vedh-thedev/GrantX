package com.grantx.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

public class GrantDto {

    @Data
    public static class DecisionRequest {
        @NotNull(message = "Proposal ID is required")
        private Long proposalId;

        @NotBlank(message = "Decision is required")
        private String decision; // APPROVED, REJECTED, REVISION_REQUESTED

        private String decisionRemarks;
        private String revisionComments;

        // For approval only
        @DecimalMin(value = "0.0")
        private BigDecimal approvedAmount;
        private String grantReference;
        private String projectPhase;
        private String sanctionDate;
    }

    @Data
    public static class UpdateFundingRequest {
        @NotNull
        private Long fundingId;

        @NotBlank
        private String grantStatus;

        private BigDecimal approvedAmount;
        private String sanctionDate;
        private String grantReference;
        private String remarks;
        private String projectPhase;
    }

    @Data
    public static class CreateMilestoneRequest {
        @NotNull(message = "Proposal ID is required")
        private Long proposalId;

        @NotBlank(message = "Title is required")
        @Size(max = 200)
        private String title;

        private String description;

        @NotNull(message = "Due date is required")
        private String dueDate;

        private String status = "NOT_STARTED";

        @Min(0) @Max(100)
        private Integer completionPercentage = 0;

        private String remarks;
    }

    @Data
    public static class UpdateMilestoneRequest {
        @NotBlank
        private String status;

        @Min(0) @Max(100)
        private Integer completionPercentage;

        private String remarks;
        private String dueDate;
    }

    @Data
    public static class DecisionSummaryResponse {
        private Long proposalId;
        private String proposalNumber;
        private String proposalTitle;
        private String studentName;
        private String studentDepartment;
        private BigDecimal requestedFunding;
        private BigDecimal aggregatedScore;
        private ReviewDto.EvaluatorStatusDto evaluator1;
        private ReviewDto.EvaluatorStatusDto evaluator2;
        private ReviewDto.ReviewResponse review1Details;
        private ReviewDto.ReviewResponse review2Details;
        private String proposalStatus;
        private ProposalDto.GrantDecisionDto latestDecision;
    }
}
