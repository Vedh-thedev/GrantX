package com.grantx.repository;

import com.grantx.entity.StatusHistory;
import com.grantx.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByProposalOrderByChangedAtAsc(Proposal proposal);
}
