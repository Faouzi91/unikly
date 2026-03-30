package com.unikly.matchingservice.application.port.out;

import com.unikly.matchingservice.domain.model.FreelancerSkillCache;
import org.springframework.data.jpa.repository.JpaRepository;
import com.unikly.matchingservice.domain.model.*;

import java.util.UUID;

public interface FreelancerSkillCacheRepository extends JpaRepository<FreelancerSkillCache, UUID> {
}
