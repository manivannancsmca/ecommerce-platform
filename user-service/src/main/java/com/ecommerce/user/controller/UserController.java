// src/main/java/com/ecommerce/user/controller/UserController.java
package com.ecommerce.user.controller;

import com.ecommerce.user.dto.request.CreateUserRequest;
import com.ecommerce.user.dto.request.UpdateUserRequest;
import com.ecommerce.user.dto.request.UpdateUserStatusRequest;
import com.ecommerce.user.dto.response.PagedResponse;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Validated
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new user.
     *
     * POST /api/users
     * 201 Created → UserResponse
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    /**
     * Get a user by ID.
     *
     * GET /api/users/{id}
     * 200 OK → UserResponse
     * 404 Not Found
     */
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        log.info("userid ::::::::: {}", id);
        return userService.getUserById(id);
    }

    /**
     * List users with pagination and sorting.
     *
     * GET /api/users?page=0&size=20&sortBy=createdAt&sortDirection=DESC
     * 200 OK → PagedResponse<UserResponse>
     */
    @GetMapping
    public PagedResponse<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return userService.getAllUsers(page, size, sortBy, sortDirection);
    }

    /**
     * Update user profile (name, phone — not email/password).
     *
     * PUT /api/users/{id}
     * 200 OK → UserResponse
     * 404 Not Found
     */
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    /**
     * Change user status (ACTIVE, INACTIVE, SUSPENDED).
     *
     * PATCH /api/users/{id}/status
     * 200 OK → UserResponse
     * 404 Not Found
     * 422 Unprocessable Entity (same status, invalid transition)
     */
    @PatchMapping("/{id}/status")
    public UserResponse updateUserStatus(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateUserStatus(id, request);
    }

    /**
     * Soft-delete a user (sets status to INACTIVE).
     *
     * DELETE /api/users/{id}
     * 204 No Content
     * 404 Not Found
     * 422 Unprocessable Entity (already inactive)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
}