package com.unikly.matchingservice.infrastructure;

import com.unikly.matchingservice.domain.FreelancerSkillCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FreelancerSkillCacheRepository extends JpaRepository<FreelancerSkillCache, UUID> {
}
