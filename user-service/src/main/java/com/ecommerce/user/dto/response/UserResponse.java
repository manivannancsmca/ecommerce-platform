// src/main/java/com/ecommerce/user/dto/response/UserResponse.java
package com.ecommerce.user.dto.response;

import com.ecommerce.user.model.enums.UserRole;
import com.ecommerce.user.model.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    UserRole role,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {}