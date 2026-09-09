package com.grantx.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposal_team_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposalTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Column(name = "register_number", nullable = false, length = 20)
    private String registerNumber;

    @Column(length = 100)
    private String department;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Column(length = 100)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
