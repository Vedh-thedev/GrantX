package com.grantx.repository;

import com.grantx.entity.ReviewAssignment;
import com.grantx.entity.Faculty;
import com.grantx.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {
    List<ReviewAssignment> findByProposal(Proposal proposal);
    List<ReviewAssignment> findByFaculty(Faculty faculty);
    List<ReviewAssignment> findByFacultyAndStatus(Faculty faculty, ReviewAssignment.AssignmentStatus status);
    Optional<ReviewAssignment> findByProposalAndEvaluatorNumber(Proposal proposal, Integer evaluatorNumber);
    boolean existsByProposalAndFaculty(Proposal proposal, Faculty faculty);
    long countByProposal(Proposal proposal);
    List<ReviewAssignment> findByProposalOrderByEvaluatorNumberAsc(Proposal proposal);
}
