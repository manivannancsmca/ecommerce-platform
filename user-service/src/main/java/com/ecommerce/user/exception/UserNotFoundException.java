// src/main/java/com/ecommerce/user/exception/UserNotFoundException.java
package com.ecommerce.user.exception;

import java.util.UUID;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(UUID id) {
        super("USER_NOT_FOUND", "User not found with id: " + id);
    }

    public UserNotFoundException(String email) {
        super("USER_NOT_FOUND", "User not found with email: " + email);
    }
}