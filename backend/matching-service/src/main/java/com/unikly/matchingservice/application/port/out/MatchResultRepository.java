package com.unikly.matchingservice.application.port.out;

import com.unikly.matchingservice.domain.model.MatchResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.unikly.matchingservice.domain.model.*;

import java.util.List;
import java.util.UUID;

public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {

    List<MatchResult> findByJobIdOrderByScoreDesc(UUID jobId, Pageable pageable);

    List<MatchResult> findByFreelancerIdOrderByScoreDesc(UUID freelancerId, Pageable pageable);

    void deleteByJobId(UUID jobId);
}
