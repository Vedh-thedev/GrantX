package com.grantx.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private ReviewAssignment assignment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "technical_comments", nullable = false, columnDefinition = "TEXT")
    private String technicalComments;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String strengths;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String weaknesses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recommendation recommendation;

    @Column(name = "additional_remarks", columnDefinition = "TEXT")
    private String additionalRemarks;

    @Column(name = "weighted_score", precision = 5, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = true;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReviewScore reviewScore;

    public enum Recommendation {
        STRONGLY_RECOMMEND, RECOMMEND, CONDITIONAL_RECOMMEND, DO_NOT_RECOMMEND,
        NEUTRAL, NOT_RECOMMEND, STRONGLY_NOT_RECOMMEND
    }

    public Review() {}

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private ReviewAssignment assignment; private Proposal proposal; private Faculty faculty;
        private String technicalComments, strengths, weaknesses, additionalRemarks;
        private Recommendation recommendation; private BigDecimal weightedScore; private Boolean isLocked = true;
        public Builder assignment(ReviewAssignment v) { this.assignment = v; return this; }
        public Builder proposal(Proposal v) { this.proposal = v; return this; }
        public Builder faculty(Faculty v) { this.faculty = v; return this; }
        public Builder technicalComments(String v) { this.technicalComments = v; return this; }
        public Builder strengths(String v) { this.strengths = v; return this; }
        public Builder weaknesses(String v) { this.weaknesses = v; return this; }
        public Builder additionalRemarks(String v) { this.additionalRemarks = v; return this; }
        public Builder recommendation(Recommendation v) { this.recommendation = v; return this; }
        public Builder weightedScore(BigDecimal v) { this.weightedScore = v; return this; }
        public Builder weightedTotal(BigDecimal v) { this.weightedScore = v; return this; }
        public Builder isLocked(Boolean v) { this.isLocked = v; return this; }
        public Review build() {
            Review r = new Review();
            r.assignment = assignment; r.proposal = proposal; r.faculty = faculty;
            r.technicalComments = technicalComments; r.strengths = strengths; r.weaknesses = weaknesses;
            r.additionalRemarks = additionalRemarks; r.recommendation = recommendation;
            r.weightedScore = weightedScore; r.isLocked = isLocked; return r;
        }
    }

    @PrePersist protected void onCreate() { submittedAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReviewAssignment getAssignment() { return assignment; }
    public void setAssignment(ReviewAssignment assignment) { this.assignment = assignment; }
    public Proposal getProposal() { return proposal; }
    public void setProposal(Proposal proposal) { this.proposal = proposal; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public String getTechnicalComments() { return technicalComments; }
    public void setTechnicalComments(String technicalComments) { this.technicalComments = technicalComments; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }
    public Recommendation getRecommendation() { return recommendation; }
    public void setRecommendation(Recommendation recommendation) { this.recommendation = recommendation; }
    public String getAdditionalRemarks() { return additionalRemarks; }
    public void setAdditionalRemarks(String additionalRemarks) { this.additionalRemarks = additionalRemarks; }
    public BigDecimal getWeightedScore() { return weightedScore; }
    public void setWeightedScore(BigDecimal weightedScore) { this.weightedScore = weightedScore; }
    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public ReviewScore getReviewScore() { return reviewScore; }
    public void setReviewScore(ReviewScore reviewScore) { this.reviewScore = reviewScore; }
}
