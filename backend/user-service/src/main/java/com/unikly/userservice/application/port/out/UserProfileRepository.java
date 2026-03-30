package com.unikly.userservice.application.port.out;

import com.unikly.userservice.domain.model.UserProfile;
import com.unikly.userservice.domain.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query(value = "SELECT * FROM user_profiles WHERE role = :role AND :skill = ANY(skills)",
           countQuery = "SELECT COUNT(*) FROM user_profiles WHERE role = :role AND :skill = ANY(skills)",
           nativeQuery = true)
    Page<UserProfile> findByRoleAndSkill(@Param("role") String role, @Param("skill") String skill, Pageable pageable);

    Page<UserProfile> findByRole(UserRole role, Pageable pageable);

    List<UserProfile> findAllByRole(UserRole role);
}
