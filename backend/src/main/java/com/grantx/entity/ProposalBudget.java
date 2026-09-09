package com.grantx.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposal_budgets")
public class ProposalBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false, unique = true)
    private Proposal proposal;

    @Column(name = "equipment_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal equipmentCost = BigDecimal.ZERO;

    @Column(name = "software_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal softwareCost = BigDecimal.ZERO;

    @Column(name = "materials_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal materialsCost = BigDecimal.ZERO;

    @Column(name = "prototype_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal prototypeCost = BigDecimal.ZERO;

    @Column(name = "testing_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal testingCost = BigDecimal.ZERO;

    @Column(name = "other_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherCost = BigDecimal.ZERO;

    @Column(name = "total_requested", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRequested = BigDecimal.ZERO;

    @Column(name = "budget_justification", columnDefinition = "TEXT")
    private String budgetJustification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProposalBudget() {}

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Proposal proposal;
        private BigDecimal equipmentCost = BigDecimal.ZERO, softwareCost = BigDecimal.ZERO,
                materialsCost = BigDecimal.ZERO, prototypeCost = BigDecimal.ZERO,
                testingCost = BigDecimal.ZERO, otherCost = BigDecimal.ZERO;
        private String budgetJustification;
        public Builder proposal(Proposal v) { this.proposal = v; return this; }
        public Builder equipmentCost(BigDecimal v) { this.equipmentCost = v; return this; }
        public Builder softwareCost(BigDecimal v) { this.softwareCost = v; return this; }
        public Builder materialsCost(BigDecimal v) { this.materialsCost = v; return this; }
        public Builder prototypeCost(BigDecimal v) { this.prototypeCost = v; return this; }
        public Builder testingCost(BigDecimal v) { this.testingCost = v; return this; }
        public Builder otherCost(BigDecimal v) { this.otherCost = v; return this; }
        public Builder budgetJustification(String v) { this.budgetJustification = v; return this; }
        public ProposalBudget build() {
            ProposalBudget b = new ProposalBudget();
            b.proposal = proposal; b.equipmentCost = equipmentCost; b.softwareCost = softwareCost;
            b.materialsCost = materialsCost; b.prototypeCost = prototypeCost; b.testingCost = testingCost;
            b.otherCost = otherCost; b.budgetJustification = budgetJustification; return b;
        }
    }

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); calculateTotal(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); calculateTotal(); }

    public void calculateTotal() {
        totalRequested = equipmentCost.add(softwareCost).add(materialsCost).add(prototypeCost).add(testingCost).add(otherCost);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proposal getProposal() { return proposal; }
    public void setProposal(Proposal proposal) { this.proposal = proposal; }
    public BigDecimal getEquipmentCost() { return equipmentCost; }
    public void setEquipmentCost(BigDecimal v) { this.equipmentCost = v; }
    public BigDecimal getSoftwareCost() { return softwareCost; }
    public void setSoftwareCost(BigDecimal v) { this.softwareCost = v; }
    public BigDecimal getMaterialsCost() { return materialsCost; }
    public void setMaterialsCost(BigDecimal v) { this.materialsCost = v; }
    public BigDecimal getPrototypeCost() { return prototypeCost; }
    public void setPrototypeCost(BigDecimal v) { this.prototypeCost = v; }
    public BigDecimal getTestingCost() { return testingCost; }
    public void setTestingCost(BigDecimal v) { this.testingCost = v; }
    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal v) { this.otherCost = v; }
    public BigDecimal getTotalRequested() { return totalRequested; }
    public void setTotalRequested(BigDecimal v) { this.totalRequested = v; }
    public String getBudgetJustification() { return budgetJustification; }
    public void setBudgetJustification(String v) { this.budgetJustification = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
