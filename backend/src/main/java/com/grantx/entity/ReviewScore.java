package com.grantx.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_scores")
public class ReviewScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Column(name = "novelty_score", nullable = false, precision = 4, scale = 1)
    private BigDecimal noveltyScore;

    @Column(name = "feasibility_score", nullable = false, precision = 4, scale = 1)
    private BigDecimal feasibilityScore;

    @Column(name = "commercial_impact_score", nullable = false, precision = 4, scale = 1)
    private BigDecimal commercialImpactScore;

    @Column(name = "weighted_total", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightedTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReviewScore() {}

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Review review; private BigDecimal noveltyScore, feasibilityScore, commercialImpactScore, weightedTotal;
        public Builder review(Review v) { this.review = v; return this; }
        public Builder noveltyScore(BigDecimal v) { this.noveltyScore = v; return this; }
        public Builder feasibilityScore(BigDecimal v) { this.feasibilityScore = v; return this; }
        public Builder commercialImpactScore(BigDecimal v) { this.commercialImpactScore = v; return this; }
        public Builder weightedTotal(BigDecimal v) { this.weightedTotal = v; return this; }
        public ReviewScore build() {
            ReviewScore rs = new ReviewScore();
            rs.review = review; rs.noveltyScore = noveltyScore;
            rs.feasibilityScore = feasibilityScore; rs.commercialImpactScore = commercialImpactScore;
            rs.weightedTotal = weightedTotal; return rs;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        calculateWeightedTotal();
    }

    public void calculateWeightedTotal() {
        BigDecimal noveltyWeight = noveltyScore.multiply(new BigDecimal("0.40"));
        BigDecimal feasibilityWeight = feasibilityScore.multiply(new BigDecimal("0.30"));
        BigDecimal commercialWeight = commercialImpactScore.multiply(new BigDecimal("0.30"));
        weightedTotal = noveltyWeight.add(feasibilityWeight).add(commercialWeight);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
    public BigDecimal getNoveltyScore() { return noveltyScore; }
    public void setNoveltyScore(BigDecimal noveltyScore) { this.noveltyScore = noveltyScore; }
    public BigDecimal getFeasibilityScore() { return feasibilityScore; }
    public void setFeasibilityScore(BigDecimal feasibilityScore) { this.feasibilityScore = feasibilityScore; }
    public BigDecimal getCommercialImpactScore() { return commercialImpactScore; }
    public void setCommercialImpactScore(BigDecimal commercialImpactScore) { this.commercialImpactScore = commercialImpactScore; }
    public BigDecimal getWeightedTotal() { return weightedTotal; }
    public void setWeightedTotal(BigDecimal weightedTotal) { this.weightedTotal = weightedTotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
