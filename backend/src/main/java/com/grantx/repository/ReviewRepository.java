package com.grantx.repository;

import com.grantx.entity.Review;
import com.grantx.entity.Faculty;
import com.grantx.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProposal(Proposal proposal);
    List<Review> findByFaculty(Faculty faculty);
    Optional<Review> findByAssignmentId(Long assignmentId);
    boolean existsByAssignmentId(Long assignmentId);
    long countByProposal(Proposal proposal);
    Optional<Review> findByProposalAndFaculty(Proposal proposal, Faculty faculty);
}
