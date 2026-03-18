package com.unikly.userservice.infrastructure;

import com.unikly.userservice.domain.UserProfile;
import com.unikly.userservice.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query("SELECT p FROM UserProfile p WHERE p.role = :role AND :skill MEMBER OF p.skills")
    Page<UserProfile> findByRoleAndSkill(UserRole role, String skill, Pageable pageable);

    Page<UserProfile> findByRole(UserRole role, Pageable pageable);
}
