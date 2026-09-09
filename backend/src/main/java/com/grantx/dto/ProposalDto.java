package com.grantx.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public class ProposalDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Title is required")
        @Size(max = 300)
        private String title;

        @NotBlank(message = "Problem statement is required")
        private String problemStatement;

        @NotBlank(message = "Proposed solution is required")
        private String proposedSolution;

        @NotBlank(message = "Abstract is required")
        private String abstrakt;

        @NotBlank(message = "Objectives are required")
        private String objectives;

        @NotBlank(message = "Technology/domain is required")
        private String technologyDomain;

        @NotBlank(message = "Innovation category is required")
        private String innovationCategory;

        private String existingSolutions;

        @NotBlank(message = "Novelty explanation is required")
        private String noveltyExplanation;

        @NotBlank(message = "Difference from existing is required")
        private String differenceFromExisting;

        @NotBlank(message = "Innovation significance is required")
        private String innovationSignificance;

        private String patentPotential = "MEDIUM";

        private Boolean noveltyDeclaration = false;

        private List<TeamMemberDto> teamMembers;

        private BudgetDto budget;

        private boolean saveDraft = false;
    }

    @Data
    public static class TeamMemberDto {
        @NotBlank(message = "Member name is required")
        private String memberName;

        @NotBlank(message = "Register number is required")
        private String registerNumber;

        private String department;
        private Integer yearOfStudy;
        private String email;
    }

    @Data
    public static class BudgetDto {
        @DecimalMin(value = "0.0") private BigDecimal equipmentCost = BigDecimal.ZERO;
        @DecimalMin(value = "0.0") private BigDecimal softwareCost = BigDecimal.ZERO;
        @DecimalMin(value = "0.0") private BigDecimal materialsCost = BigDecimal.ZERO;
        @DecimalMin(value = "0.0") private BigDecimal prototypeCost = BigDecimal.ZERO;
        @DecimalMin(value = "0.0") private BigDecimal testingCost = BigDecimal.ZERO;
        @DecimalMin(value = "0.0") private BigDecimal otherCost = BigDecimal.ZERO;
        private String budgetJustification;
    }

    @Data
    public static class ProposalResponse {
        private Long id;
        private String proposalNumber;
        private String title;
        private String status;
        private String innovationCategory;
        private String technologyDomain;
        private String patentPotential;
        private String studentName;
        private String studentDepartment;
        private String studentRegisterNumber;
        private BigDecimal aggregatedScore;
        private BigDecimal totalBudgetRequested;
        private String submissionDate;
        private String createdAt;
        private String updatedAt;
        private int reviewCount;
        private int teamMemberCount;
    }

    @Data
    public static class ProposalDetailResponse extends ProposalResponse {
        private String problemStatement;
        private String proposedSolution;
        private String abstrakt;
        private String objectives;
        private String existingSolutions;
        private String noveltyExplanation;
        private String differenceFromExisting;
        private String innovationSignificance;
        private Boolean noveltyDeclaration;
        private List<TeamMemberDto> teamMembers;
        private BudgetDetailDto budget;
        private List<StatusHistoryDto> statusHistory;
        private List<MilestoneDto> milestones;
        private GrantFundingDto funding;
        private GrantDecisionDto latestDecision;
    }

    @Data
    public static class BudgetDetailDto {
        private BigDecimal equipmentCost;
        private BigDecimal softwareCost;
        private BigDecimal materialsCost;
        private BigDecimal prototypeCost;
        private BigDecimal testingCost;
        private BigDecimal otherCost;
        private BigDecimal totalRequested;
        private String budgetJustification;
    }

    @Data
    public static class StatusHistoryDto {
        private String previousStatus;
        private String newStatus;
        private String changedBy;
        private String remarks;
        private String changedAt;
    }

    @Data
    public static class MilestoneDto {
        private Long id;
        private String title;
        private String description;
        private String dueDate;
        private String status;
        private Integer completionPercentage;
        private String remarks;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    public static class GrantFundingDto {
        private Long id;
        private BigDecimal requestedAmount;
        private BigDecimal approvedAmount;
        private String grantStatus;
        private String sanctionDate;
        private String grantReference;
        private String remarks;
        private String projectPhase;
    }

    @Data
    public static class GrantDecisionDto {
        private Long id;
        private String decision;
        private String decisionRemarks;
        private String revisionComments;
        private BigDecimal aggregatedScoreAtDecision;
        private String decisionDate;
        private String decidedBy;
    }
}
