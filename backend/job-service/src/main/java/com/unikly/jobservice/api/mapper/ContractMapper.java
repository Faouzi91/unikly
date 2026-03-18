package com.unikly.jobservice.api.mapper;

import com.unikly.jobservice.api.dto.ContractResponse;
import com.unikly.jobservice.domain.Contract;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    ContractResponse toResponse(Contract entity);
}
