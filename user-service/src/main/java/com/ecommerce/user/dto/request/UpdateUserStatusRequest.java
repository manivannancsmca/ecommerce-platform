// src/main/java/com/ecommerce/user/dto/request/UpdateUserStatusRequest.java
package com.ecommerce.user.dto.request;

import com.ecommerce.user.model.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

    @NotNull(message = "Status is required")
    UserStatus status
) {}