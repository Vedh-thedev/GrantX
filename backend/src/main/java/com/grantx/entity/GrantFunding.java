package com.grantx.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "grant_funding")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrantFunding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "decision_id", nullable = false)
    private GrantDecision decision;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_status", nullable = false)
    @Builder.Default
    private GrantStatus grantStatus = GrantStatus.PENDING;

    @Column(name = "sanction_date")
    private LocalDate sanctionDate;

    @Column(name = "grant_reference", length = 50)
    private String grantReference;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "project_phase", length = 100)
    private String projectPhase;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum GrantStatus {
        PENDING, APPROVED, SANCTIONED, PARTIALLY_APPROVED, REJECTED, COMPLETED
    }
}
