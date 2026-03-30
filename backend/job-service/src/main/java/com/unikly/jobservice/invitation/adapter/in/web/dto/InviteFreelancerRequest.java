package com.unikly.jobservice.invitation.adapter.in.web.dto;

import java.util.UUID;

public record InviteFreelancerRequest(UUID freelancerId, String message) {}
