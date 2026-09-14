// src/main/java/com/ecommerce/product/aspect/LoggingAspect.java
package com.ecommerce.product.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final long SLOW_THRESHOLD_MS = 3000;

    @Pointcut("within(com.ecommerce.product.controller..*) || within(com.ecommerce.product.service..*)")
    public void applicationLayer() {}

    @Around("applicationLayer()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());
        String sig = joinPoint.getSignature().toShortString();

        log.debug("→ {}", sig);
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("← {} ({} ms)", sig, elapsed);
            if (elapsed > SLOW_THRESHOLD_MS) {
                log.warn("SLOW: {} took {} ms", sig, elapsed);
            }
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("✗ {} after {} ms: [{}] {}", sig, elapsed,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}