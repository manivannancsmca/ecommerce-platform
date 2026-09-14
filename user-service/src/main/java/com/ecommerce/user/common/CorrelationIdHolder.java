// src/main/java/com/ecommerce/user/common/CorrelationIdHolder.java
package com.ecommerce.user.common;

import org.slf4j.MDC;

/**
 * Thread-local holder for the correlation ID.
 * Accessible anywhere in the service via CorrelationIdHolder.getCorrelationId().
 * The MDC key "correlationId" is referenced in the logging pattern.
 */
public final class CorrelationIdHolder {

    private static final String KEY = "correlationId";

    private CorrelationIdHolder() {}

    public static String getCorrelationId() {
        return MDC.get(KEY);
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(KEY, correlationId);
        }
    }

    public static void clear() {
        MDC.remove(KEY);
    }
}