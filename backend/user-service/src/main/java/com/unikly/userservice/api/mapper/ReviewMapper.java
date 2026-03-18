package com.unikly.userservice.api.mapper;

import com.unikly.userservice.api.dto.ReviewResponse;
import com.unikly.userservice.domain.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toResponse(Review entity);
}
