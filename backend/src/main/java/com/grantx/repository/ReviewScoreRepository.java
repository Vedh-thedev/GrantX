package com.grantx.repository;

import com.grantx.entity.ReviewScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReviewScoreRepository extends JpaRepository<ReviewScore, Long> {
    Optional<ReviewScore> findByReviewId(Long reviewId);
}
