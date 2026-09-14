// src/main/java/com/ecommerce/product/common/CorrelationIdHolder.java
package com.ecommerce.product.common;

import org.slf4j.MDC;

public final class CorrelationIdHolder {

    private static final String KEY = "correlationId";

    private CorrelationIdHolder() {}

    public static String getCorrelationId() {
        return MDC.get(KEY);
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) MDC.put(KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(KEY);
    }
}