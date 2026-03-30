package com.unikly.jobservice.adapter.in.web.dto;

import java.util.UUID;

public record InviteFreelancerRequest(UUID freelancerId, String message) {}
