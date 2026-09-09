package com.grantx.repository;

import com.grantx.entity.GrantFunding;
import com.grantx.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrantFundingRepository extends JpaRepository<GrantFunding, Long> {
    Optional<GrantFunding> findByProposal(Proposal proposal);
    List<GrantFunding> findByGrantStatus(GrantFunding.GrantStatus grantStatus);

    @Query("SELECT COALESCE(SUM(g.requestedAmount), 0) FROM GrantFunding g")
    BigDecimal sumTotalRequested();

    @Query("SELECT COALESCE(SUM(g.approvedAmount), 0) FROM GrantFunding g WHERE g.approvedAmount IS NOT NULL")
    BigDecimal sumTotalApproved();
}
