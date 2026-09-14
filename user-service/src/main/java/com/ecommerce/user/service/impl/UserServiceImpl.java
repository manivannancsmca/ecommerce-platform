// src/main/java/com/ecommerce/user/service/impl/UserServiceImpl.java
package com.ecommerce.user.service.impl;

import com.ecommerce.user.dto.request.CreateUserRequest;
import com.ecommerce.user.dto.request.UpdateUserRequest;
import com.ecommerce.user.dto.request.UpdateUserStatusRequest;
import com.ecommerce.user.dto.response.PagedResponse;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.exception.BusinessException;
import com.ecommerce.user.exception.EmailAlreadyExistsException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.model.entity.User;
import com.ecommerce.user.model.enums.UserRole;
import com.ecommerce.user.model.enums.UserStatus;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "email", "firstName", "lastName", "status"
    );

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = userMapper.toEntity(request);
        user.setId(UUID.randomUUID());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        log.info("Created user {} with email {}", saved.getId(), saved.getEmail());

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(int page, int size,
                                                   String sortBy, String sortDirection) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException("INVALID_SORT_FIELD",
                    "Sort field '" + sortBy + "' is not allowed. Allowed: " + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_SORT_DIRECTION",
                    "Sort direction must be ASC or DESC, got: " + sortDirection);
        }

        int boundedSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, boundedSize, Sort.by(direction, sortBy));

        Page<User> userPage = userRepository.findAll(pageable);

        List<UserResponse> content = userPage.getContent().stream()
                .map(userMapper::toResponse)
                .toList();

        return PagedResponse.of(content, userPage);
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);
        userMapper.updateEntity(user, request);

        User saved = userRepository.save(user);
        log.info("Updated user {}", id);

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateUserStatus(UUID id, UpdateUserStatusRequest request) {
        User user = findUserOrThrow(id);

        if (user.getStatus() == request.status()) {
            throw new BusinessException("STATUS_UNCHANGED",
                    "User is already in " + request.status() + " status");
        }

        UserStatus previousStatus = user.getStatus();
        user.setStatus(request.status());

        User saved = userRepository.save(user);
        log.info("User {} status changed: {} → {}", id, previousStatus, request.status());

        return userMapper.toResponse(saved);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = findUserOrThrow(id);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException("USER_ALREADY_INACTIVE",
                    "User " + id + " is already inactive");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("Soft-deleted user {}", id);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }
}