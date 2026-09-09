package com.grantx.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_assignments")
public class ReviewAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    @Column(name = "evaluator_number", nullable = false)
    private Integer evaluatorNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Review review;

    public enum AssignmentStatus { PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, OVERDUE }

    public ReviewAssignment() {}

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Proposal proposal; private Faculty faculty; private User assignedBy;
        private Integer evaluatorNumber; private AssignmentStatus status = AssignmentStatus.ASSIGNED;
        private LocalDateTime deadline;
        public Builder proposal(Proposal v) { this.proposal = v; return this; }
        public Builder faculty(Faculty v) { this.faculty = v; return this; }
        public Builder assignedBy(User v) { this.assignedBy = v; return this; }
        public Builder evaluatorNumber(Integer v) { this.evaluatorNumber = v; return this; }
        public Builder status(AssignmentStatus v) { this.status = v; return this; }
        public Builder deadline(LocalDateTime v) { this.deadline = v; return this; }
        public Builder deadline(java.time.LocalDate v) { this.deadline = v != null ? v.atStartOfDay() : null; return this; }
        public ReviewAssignment build() {
            ReviewAssignment r = new ReviewAssignment();
            r.proposal = proposal; r.faculty = faculty; r.assignedBy = assignedBy; r.evaluatorNumber = evaluatorNumber;
            r.status = status; r.deadline = deadline; return r;
        }
    }

    @PrePersist protected void onCreate() { assignedAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proposal getProposal() { return proposal; }
    public void setProposal(Proposal proposal) { this.proposal = proposal; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }
    public Integer getEvaluatorNumber() { return evaluatorNumber; }
    public void setEvaluatorNumber(Integer evaluatorNumber) { this.evaluatorNumber = evaluatorNumber; }
    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public void setDeadline(java.time.LocalDate deadline) { this.deadline = deadline != null ? deadline.atStartOfDay() : null; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
}
