package com.drawingdiary.backend.domain.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LoginIdValidator implements ConstraintValidator<ValidLoginId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        return LoginIdPolicy.violation(value)
                .map(message -> {
                    context.disableDefaultConstraintViolation();
                    // 사용자 입력이 메시지에 섞이므로 메시지 템플릿 문법({...}, ${...})으로
                    // 해석되지 않게 이스케이프한다.
                    context.buildConstraintViolationWithTemplate(escape(message)).addConstraintViolation();
                    return false;
                })
                .orElse(true);
    }

    private String escape(String message) {
        StringBuilder escaped = new StringBuilder(message.length());
        for (char ch : message.toCharArray()) {
            if (ch == '\\' || ch == '{' || ch == '}' || ch == '$') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }
}
