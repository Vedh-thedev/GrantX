package com.grantx.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grant_decisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrantDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "decided_by", nullable = false)
    private User decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision;

    @Column(name = "decision_remarks", columnDefinition = "TEXT")
    private String decisionRemarks;

    @Column(name = "revision_comments", columnDefinition = "TEXT")
    private String revisionComments;

    @Column(name = "aggregated_score_at_decision", precision = 5, scale = 2)
    private BigDecimal aggregatedScoreAtDecision;

    @Column(name = "decision_date", nullable = false)
    private LocalDateTime decisionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (decisionDate == null) decisionDate = LocalDateTime.now();
    }

    public enum Decision {
        APPROVED, REJECTED, REVISION_REQUESTED
    }
}
