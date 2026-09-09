package com.grantx.repository;

import com.grantx.entity.GrantDecision;
import com.grantx.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrantDecisionRepository extends JpaRepository<GrantDecision, Long> {
    List<GrantDecision> findByProposalOrderByCreatedAtDesc(Proposal proposal);
    Optional<GrantDecision> findTopByProposalOrderByCreatedAtDesc(Proposal proposal);
}
