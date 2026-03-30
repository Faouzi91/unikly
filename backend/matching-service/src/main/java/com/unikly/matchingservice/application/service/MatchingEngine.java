package com.unikly.matchingservice.application.service;

import com.unikly.matchingservice.domain.model.MatchResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MatchingEngine {

    List<MatchResult> computeMatches(UUID jobId,
                                     String title,
                                     String description,
                                     List<String> requiredSkills,
                                     BigDecimal budget);
}
