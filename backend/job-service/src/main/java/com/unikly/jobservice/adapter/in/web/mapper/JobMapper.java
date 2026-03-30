package com.unikly.jobservice.adapter.in.web.mapper;

import com.unikly.jobservice.adapter.in.web.dto.CreateJobRequest;
import com.unikly.jobservice.adapter.in.web.dto.JobResponse;
import com.unikly.jobservice.adapter.in.web.dto.UpdateJobRequest;
import com.unikly.jobservice.domain.model.Job;
import com.unikly.jobservice.domain.model.JobStatus;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", imports = JobStatus.class)
public interface JobMapper {

    JobResponse toResponse(Job job);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "clientId",  ignore = true)
    @Mapping(target = "status",    expression = "java(JobStatus.DRAFT)")
    @Mapping(target = "version",   constant = "1")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Job toEntity(CreateJobRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "clientId",  ignore = true)
    @Mapping(target = "currency",  ignore = true)
    @Mapping(target = "status",    ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateJobRequest request, @MappingTarget Job job);
}
