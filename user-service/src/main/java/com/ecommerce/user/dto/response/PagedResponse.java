// src/main/java/com/ecommerce/user/dto/response/PagedResponse.java
package com.ecommerce.user.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {
    public static <T> PagedResponse<T> of(List<T> content, Page<?> pageMetadata) {
        return new PagedResponse<>(
            content,
            pageMetadata.getNumber(),
            pageMetadata.getSize(),
            pageMetadata.getTotalElements(),
            pageMetadata.getTotalPages(),
            pageMetadata.isLast()
        );
    }
}