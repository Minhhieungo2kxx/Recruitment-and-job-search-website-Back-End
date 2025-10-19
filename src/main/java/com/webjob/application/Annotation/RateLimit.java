package com.webjob.application.Annotation;
import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxRequests();            // Số lượng request tối đa
    int timeWindowSeconds();     // Trong bao lâu (tính bằng giây)
    String keyType(); // IP hoặc TOKEN
}
