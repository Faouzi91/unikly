package com.unikly.userservice.adapter.in.web.mapper;

import com.unikly.userservice.adapter.in.web.dto.ReviewResponse;
import com.unikly.userservice.domain.model.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toResponse(Review entity);
}
