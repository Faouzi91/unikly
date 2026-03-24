package com.unikly.jobservice.application.mapper;

import com.unikly.jobservice.api.dto.ProposalResponse;
import com.unikly.jobservice.api.dto.SubmitProposalRequest;
import com.unikly.jobservice.domain.Proposal;
import com.unikly.jobservice.domain.ProposalStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = ProposalStatus.class)
public interface ProposalMapper {

    ProposalResponse toResponse(Proposal proposal);

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "jobId",        ignore = true)
    @Mapping(target = "freelancerId", ignore = true)
    @Mapping(target = "status",       expression = "java(ProposalStatus.SUBMITTED)")
    @Mapping(target = "jobVersion",   ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "version",      ignore = true)
    Proposal toEntity(SubmitProposalRequest request);
}
