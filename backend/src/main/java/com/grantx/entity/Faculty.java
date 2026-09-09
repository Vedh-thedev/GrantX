package com.grantx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "employee_id", nullable = false, unique = true, length = 20)
    private String employeeId;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(length = 200)
    private String specialization;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "faculty", fetch = FetchType.LAZY)
    private List<ReviewAssignment> reviewAssignments;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Faculty() {}

    public static FacultyBuilder builder() { return new FacultyBuilder(); }
    public static class FacultyBuilder {
        private User user; private String employeeId, department, designation, specialization;
        public FacultyBuilder user(User v) { this.user = v; return this; }
        public FacultyBuilder employeeId(String v) { this.employeeId = v; return this; }
        public FacultyBuilder department(String v) { this.department = v; return this; }
        public FacultyBuilder designation(String v) { this.designation = v; return this; }
        public FacultyBuilder specialization(String v) { this.specialization = v; return this; }
        public Faculty build() {
            Faculty f = new Faculty(); f.user = user; f.employeeId = employeeId;
            f.department = department; f.designation = designation; f.specialization = specialization; return f;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public List<ReviewAssignment> getReviewAssignments() { return reviewAssignments; }
    public void setReviewAssignments(List<ReviewAssignment> v) { this.reviewAssignments = v; }
}
