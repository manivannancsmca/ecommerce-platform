// src/main/java/com/ecommerce/user/aspect/LoggingAspect.java
package com.ecommerce.user.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting logging concern for controller and service layers.
 *
 * Logs:
 *   - Method entry (DEBUG)
 *   - Method exit with execution time (DEBUG)
 *   - Slow method warnings over 3 seconds (WARN)
 *   - Exceptions with timing (ERROR)
 *
 * Does NOT log method arguments — avoids leaking sensitive data (passwords,
 * tokens, PII). Parameter-level logging should be done explicitly in service
 * methods where values are known to be safe.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final long SLOW_METHOD_THRESHOLD_MS = 3000;

    @Pointcut("within(com.ecommerce.user.controller..*)")
    public void controllerLayer() {}

    @Pointcut("within(com.ecommerce.user.service..*)")
    public void serviceLayer() {}

    @Around("controllerLayer() || serviceLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());

        String signature = joinPoint.getSignature().toShortString();
        log.debug("Entering: {}", signature);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            log.debug("Exiting: {} ({} ms)", signature, elapsed);

            if (elapsed > SLOW_METHOD_THRESHOLD_MS) {
                log.warn("Slow method: {} took {} ms (threshold: {} ms)",
                        signature, elapsed, SLOW_METHOD_THRESHOLD_MS);
            }

            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Exception in {} after {} ms: [{}] {}",
                    signature, elapsed, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}