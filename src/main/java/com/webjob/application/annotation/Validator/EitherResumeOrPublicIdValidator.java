package com.webjob.application.annotation.Validator;

import com.webjob.application.annotation.EitherResumeOrPublicId;
import com.webjob.application.dto.Request.ApplyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EitherResumeOrPublicIdValidator implements ConstraintValidator<EitherResumeOrPublicId, ApplyRequest> {
    @Override
    public boolean isValid(ApplyRequest value, ConstraintValidatorContext context) {

        boolean hasResumeId = value.getResumeId() != null;
        boolean hasPublicId = value.getPublicId() != null
                && !value.getPublicId().isBlank();

        if (hasResumeId ^ hasPublicId) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(
                        "Chỉ được chọn resumeId hoặc publicId")
                .addPropertyNode("resumeId")
                .addConstraintViolation();

        context.buildConstraintViolationWithTemplate(
                        "Chỉ được chọn resumeId hoặc publicId")
                .addPropertyNode("publicId")
                .addConstraintViolation();

        return false;
    }
}
