package com.unikly.jobservice.adapter.in.web.mapper;

import com.unikly.jobservice.adapter.in.web.dto.ProposalResponse;
import com.unikly.jobservice.adapter.in.web.dto.SubmitProposalRequest;
import com.unikly.jobservice.domain.model.Proposal;
import com.unikly.jobservice.domain.model.ProposalStatus;
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
