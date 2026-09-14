// src/main/java/com/ecommerce/user/exception/EmailAlreadyExistsException.java
package com.ecommerce.user.exception;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException(String email) {
        super("EMAIL_ALREADY_EXISTS", "A user with email '" + email + "' already exists");
    }
}