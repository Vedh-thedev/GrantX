package com.grantx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "register_number", nullable = false, unique = true, length = 20)
    private String studentId;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "year_of_study", length = 10)
    private String year;

    @Column(length = 100)
    private String section;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<Proposal> proposals;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Student() {}

    public static StudentBuilder builder() { return new StudentBuilder(); }
    public static class StudentBuilder {
        private User user; private String studentId, department, year, section;
        public StudentBuilder user(User v) { this.user = v; return this; }
        public StudentBuilder studentId(String v) { this.studentId = v; return this; }
        public StudentBuilder registerNumber(String v) { this.studentId = v; return this; }
        public StudentBuilder department(String v) { this.department = v; return this; }
        public StudentBuilder year(String v) { this.year = v; return this; }
        public StudentBuilder yearOfStudy(int v) { this.year = String.valueOf(v); return this; }
        public StudentBuilder yearOfStudy(Integer v) { this.year = v != null ? String.valueOf(v) : null; return this; }
        public StudentBuilder section(String v) { this.section = v; return this; }
        public Student build() {
            Student s = new Student(); s.user = user; s.studentId = studentId;
            s.department = department; s.year = year; s.section = section; return s;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getRegisterNumber() { return studentId; }
    public void setRegisterNumber(String registerNumber) { this.studentId = registerNumber; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public List<Proposal> getProposals() { return proposals; }
    public void setProposals(List<Proposal> proposals) { this.proposals = proposals; }
}
