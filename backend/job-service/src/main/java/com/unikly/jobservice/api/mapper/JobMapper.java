package com.unikly.jobservice.api.mapper;

import com.unikly.jobservice.api.dto.JobResponse;
import com.unikly.jobservice.domain.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "proposalCount", ignore = true)
    JobResponse toResponse(Job entity);

    default JobResponse toResponse(Job entity, long proposalCount) {
        return new JobResponse(
                entity.getId(),
                entity.getClientId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getBudget(),
                entity.getCurrency(),
                entity.getSkills(),
                entity.getStatus(),
                proposalCount,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
