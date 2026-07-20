package com.webjob.application.annotation;

import com.webjob.application.annotation.Validator.EitherResumeOrPublicIdValidator;
import jakarta.validation.Constraint;
import org.springframework.messaging.handler.annotation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EitherResumeOrPublicIdValidator.class)
@Documented
public @interface EitherResumeOrPublicId {
    String message() default "Chỉ được phép truyền resumeId hoặc publicId";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
