package com.unikly.jobservice.api.mapper;

import com.unikly.jobservice.api.dto.ProposalResponse;
import com.unikly.jobservice.domain.Proposal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProposalMapper {

    ProposalResponse toResponse(Proposal entity);
}
