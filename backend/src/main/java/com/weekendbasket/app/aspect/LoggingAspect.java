package com.weekendbasket.app.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LogManager.getLogger(LoggingAspect.class);

    // Pointcut for all controllers and services
    @Pointcut("within(com.weekendbasket.app.controller..*) || within(com.weekendbasket.app.service..*)")
    public void applicationLayer() {}

    // Log method entry + exit with timing
    @Around("applicationLayer()")
    public Object logAround(ProceedingJoinPoint jp) throws Throwable {
        String method = jp.getSignature().getDeclaringTypeName() + "." + jp.getSignature().getName();

        if (log.isDebugEnabled()) {
            log.debug("→ {} args={}", method, Arrays.toString(jp.getArgs()));
        }

        long start = System.currentTimeMillis();
        try {
            Object result = jp.proceed();
            log.debug("← {} ({}ms)", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            // This will be caught below by @AfterThrowing too, but log here for timing
            log.error("✗ {} failed after {}ms — {}: {}",
                    method,
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw ex;
        }
    }

    // Log full stack trace for any exception thrown from controller or service
    @AfterThrowing(pointcut = "applicationLayer()", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        log.error("EXCEPTION in {}.{}() — {}",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                ex.getMessage(),
                ex);
    }
}
