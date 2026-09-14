// src/main/java/com/ecommerce/user/service/UserService.java
package com.ecommerce.user.service;

import com.ecommerce.user.dto.request.CreateUserRequest;
import com.ecommerce.user.dto.request.UpdateUserRequest;
import com.ecommerce.user.dto.request.UpdateUserStatusRequest;
import com.ecommerce.user.dto.response.PagedResponse;
import com.ecommerce.user.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    PagedResponse<UserResponse> getAllUsers(int page, int size, String sortBy, String sortDirection);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    UserResponse updateUserStatus(UUID id, UpdateUserStatusRequest request);

    void deleteUser(UUID id);
}