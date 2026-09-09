package com.grantx.repository;

import com.grantx.entity.Milestone;
import com.grantx.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByProposalOrderByDueDateAsc(Proposal proposal);
    List<Milestone> findByProposalAndStatus(Proposal proposal, Milestone.MilestoneStatus status);
    long countByProposal(Proposal proposal);
    long countByProposalAndStatus(Proposal proposal, Milestone.MilestoneStatus status);
}
