package com.unikly.common.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MatchEntry(
        UUID freelancerId,
        BigDecimal score,
        List<String> matchedSkills
) {
}
