// src/main/java/com/ecommerce/user/mapper/UserMapper.java
package com.ecommerce.user.mapper;

import com.ecommerce.user.dto.request.CreateUserRequest;
import com.ecommerce.user.dto.request.UpdateUserRequest;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /**
     * Maps create request → entity.
     * Password hash and ID are set in the service layer (not here).
     */
    public User toEntity(CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        return user;
    }

    /**
     * Maps entity → response.
     * Password hash is deliberately excluded.
     */
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    /**
     * Updates mutable fields on an existing entity.
     * Email and password are NOT updated here — they require separate flows.
     */
    public void updateEntity(User user, UpdateUserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
    }
}