package com.grantx.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "proposals")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_number", nullable = false, unique = true, length = 30)
    private String proposalNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "problem_statement", nullable = false, columnDefinition = "TEXT")
    private String problemStatement;

    @Column(name = "proposed_solution", nullable = false, columnDefinition = "TEXT")
    private String proposedSolution;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String abstrakt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "technology_domain", nullable = false, length = 200)
    private String technologyDomain;

    @Column(name = "innovation_category", nullable = false, length = 100)
    private String innovationCategory;

    @Column(name = "existing_solutions", columnDefinition = "TEXT")
    private String existingSolutions;

    @Column(name = "novelty_explanation", nullable = false, columnDefinition = "TEXT")
    private String noveltyExplanation;

    @Column(name = "difference_from_existing", nullable = false, columnDefinition = "TEXT")
    private String differenceFromExisting;

    @Column(name = "innovation_significance", nullable = false, columnDefinition = "TEXT")
    private String innovationSignificance;

    @Enumerated(EnumType.STRING)
    @Column(name = "patent_potential", nullable = false)
    private PatentPotential patentPotential = PatentPotential.MEDIUM;

    @Column(name = "novelty_declaration", nullable = false)
    private Boolean noveltyDeclaration = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(name = "aggregated_score", precision = 5, scale = 2)
    private BigDecimal aggregatedScore;

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProposalTeamMember> teamMembers;

    @OneToOne(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProposalBudget budget;

    @OneToMany(mappedBy = "proposal", fetch = FetchType.LAZY)
    private List<ReviewAssignment> reviewAssignments;

    @OneToMany(mappedBy = "proposal", fetch = FetchType.LAZY)
    private List<StatusHistory> statusHistories;

    @OneToMany(mappedBy = "proposal", fetch = FetchType.LAZY)
    private List<Milestone> milestones;

    public enum ProposalStatus {
        DRAFT, SUBMITTED, PENDING_EVALUATOR_ASSIGNMENT,
        EVALUATORS_ASSIGNED, UNDER_REVIEW, REVIEW_1_COMPLETED,
        REVIEW_2_COMPLETED, SCORES_AGGREGATED, PENDING_GRANT_DECISION,
        PENDING_REVISION, RESUBMITTED, APPROVED, REJECTED,
        GRANT_SANCTIONED, MILESTONE_TRACKING, COMPLETED
    }

    public enum PatentPotential { HIGH, MEDIUM, LOW, NONE }

    public Proposal() {}

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String proposalNumber; private Student student; private String title;
        private String problemStatement, proposedSolution, abstrakt, objectives;
        private String technologyDomain, innovationCategory, existingSolutions;
        private String noveltyExplanation, differenceFromExisting, innovationSignificance;
        private PatentPotential patentPotential = PatentPotential.MEDIUM;
        private Boolean noveltyDeclaration = false;
        private ProposalStatus status = ProposalStatus.DRAFT;
        private BigDecimal aggregatedScore; private LocalDateTime submissionDate;

        public Builder proposalNumber(String v) { this.proposalNumber = v; return this; }
        public Builder student(Student v) { this.student = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder problemStatement(String v) { this.problemStatement = v; return this; }
        public Builder proposedSolution(String v) { this.proposedSolution = v; return this; }
        public Builder abstrakt(String v) { this.abstrakt = v; return this; }
        public Builder objectives(String v) { this.objectives = v; return this; }
        public Builder technologyDomain(String v) { this.technologyDomain = v; return this; }
        public Builder innovationCategory(String v) { this.innovationCategory = v; return this; }
        public Builder existingSolutions(String v) { this.existingSolutions = v; return this; }
        public Builder noveltyExplanation(String v) { this.noveltyExplanation = v; return this; }
        public Builder differenceFromExisting(String v) { this.differenceFromExisting = v; return this; }
        public Builder innovationSignificance(String v) { this.innovationSignificance = v; return this; }
        public Builder patentPotential(PatentPotential v) { this.patentPotential = v; return this; }
        public Builder noveltyDeclaration(Boolean v) { this.noveltyDeclaration = v; return this; }
        public Builder status(ProposalStatus v) { this.status = v; return this; }
        public Builder aggregatedScore(BigDecimal v) { this.aggregatedScore = v; return this; }
        public Builder submissionDate(LocalDateTime v) { this.submissionDate = v; return this; }

        public Proposal build() {
            Proposal p = new Proposal();
            p.proposalNumber = proposalNumber; p.student = student; p.title = title;
            p.problemStatement = problemStatement; p.proposedSolution = proposedSolution;
            p.abstrakt = abstrakt; p.objectives = objectives;
            p.technologyDomain = technologyDomain; p.innovationCategory = innovationCategory;
            p.existingSolutions = existingSolutions; p.noveltyExplanation = noveltyExplanation;
            p.differenceFromExisting = differenceFromExisting; p.innovationSignificance = innovationSignificance;
            p.patentPotential = patentPotential; p.noveltyDeclaration = noveltyDeclaration;
            p.status = status; p.aggregatedScore = aggregatedScore; p.submissionDate = submissionDate;
            return p;
        }
    }

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProposalNumber() { return proposalNumber; }
    public void setProposalNumber(String proposalNumber) { this.proposalNumber = proposalNumber; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
    public String getProposedSolution() { return proposedSolution; }
    public void setProposedSolution(String proposedSolution) { this.proposedSolution = proposedSolution; }
    public String getAbstrakt() { return abstrakt; }
    public void setAbstrakt(String abstrakt) { this.abstrakt = abstrakt; }
    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }
    public String getTechnologyDomain() { return technologyDomain; }
    public void setTechnologyDomain(String technologyDomain) { this.technologyDomain = technologyDomain; }
    public String getInnovationCategory() { return innovationCategory; }
    public void setInnovationCategory(String innovationCategory) { this.innovationCategory = innovationCategory; }
    public String getExistingSolutions() { return existingSolutions; }
    public void setExistingSolutions(String existingSolutions) { this.existingSolutions = existingSolutions; }
    public String getNoveltyExplanation() { return noveltyExplanation; }
    public void setNoveltyExplanation(String noveltyExplanation) { this.noveltyExplanation = noveltyExplanation; }
    public String getDifferenceFromExisting() { return differenceFromExisting; }
    public void setDifferenceFromExisting(String differenceFromExisting) { this.differenceFromExisting = differenceFromExisting; }
    public String getInnovationSignificance() { return innovationSignificance; }
    public void setInnovationSignificance(String innovationSignificance) { this.innovationSignificance = innovationSignificance; }
    public PatentPotential getPatentPotential() { return patentPotential; }
    public void setPatentPotential(PatentPotential patentPotential) { this.patentPotential = patentPotential; }
    public Boolean getNoveltyDeclaration() { return noveltyDeclaration; }
    public void setNoveltyDeclaration(Boolean noveltyDeclaration) { this.noveltyDeclaration = noveltyDeclaration; }
    public ProposalStatus getStatus() { return status; }
    public void setStatus(ProposalStatus status) { this.status = status; }
    public BigDecimal getAggregatedScore() { return aggregatedScore; }
    public void setAggregatedScore(BigDecimal aggregatedScore) { this.aggregatedScore = aggregatedScore; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ProposalTeamMember> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(List<ProposalTeamMember> teamMembers) { this.teamMembers = teamMembers; }
    public ProposalBudget getBudget() { return budget; }
    public void setBudget(ProposalBudget budget) { this.budget = budget; }
    public List<ReviewAssignment> getReviewAssignments() { return reviewAssignments; }
    public void setReviewAssignments(List<ReviewAssignment> reviewAssignments) { this.reviewAssignments = reviewAssignments; }
    public List<StatusHistory> getStatusHistories() { return statusHistories; }
    public void setStatusHistories(List<StatusHistory> statusHistories) { this.statusHistories = statusHistories; }
    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }
}
