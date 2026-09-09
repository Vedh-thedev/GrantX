package com.grantx.repository;

import com.grantx.entity.Proposal;
import com.grantx.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByStudent(Student student);
    List<Proposal> findByStudentOrderByCreatedAtDesc(Student student);
    List<Proposal> findByStatus(Proposal.ProposalStatus status);
    List<Proposal> findByStatusOrderByCreatedAtDesc(Proposal.ProposalStatus status);
    Optional<Proposal> findByProposalNumber(String proposalNumber);
    boolean existsByProposalNumber(String proposalNumber);

    @Query("SELECT COUNT(p) FROM Proposal p WHERE p.student = :student AND p.status = :status")
    long countByStudentAndStatus(@Param("student") Student student, @Param("status") Proposal.ProposalStatus status);

    @Query("SELECT COUNT(p) FROM Proposal p WHERE p.student = :student")
    long countByStudent(@Param("student") Student student);

    @Query("SELECT p FROM Proposal p WHERE p.status NOT IN ('DRAFT') ORDER BY p.createdAt DESC")
    List<Proposal> findAllSubmittedProposals();

    @Query("SELECT p FROM Proposal p WHERE p.status = 'SUBMITTED' OR p.status = 'PENDING_EVALUATOR_ASSIGNMENT' ORDER BY p.submissionDate ASC")
    List<Proposal> findProposalsPendingAssignment();

    @Query("SELECT p FROM Proposal p WHERE p.status IN ('REVIEW_2_COMPLETED','SCORES_AGGREGATED','PENDING_GRANT_DECISION')")
    List<Proposal> findProposalsPendingDecision();
}
