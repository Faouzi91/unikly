package com.unikly.userservice.api.mapper;

import com.unikly.userservice.api.dto.UserProfileRequest;
import com.unikly.userservice.api.dto.UserProfileResponse;
import com.unikly.userservice.domain.UserProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    UserProfileResponse toResponse(UserProfile entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UserProfileRequest request, @MappingTarget UserProfile entity);
}
