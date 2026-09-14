// src/main/java/com/ecommerce/user/dto/request/UpdateUserRequest.java
package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Partial update — email and password are deliberately excluded.
 * Email changes require verification (future security phase).
 * Password changes have their own dedicated endpoint (future).
 */
public record UpdateUserRequest(

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName,

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phone
) {}