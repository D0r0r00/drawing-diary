package com.drawingdiary.backend.domain.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 아이디 규칙 검사. 메시지는 LoginIdPolicy가 위반 사유별로 만들어 넣으므로 여기 message는
 * 쓰이지 않는다(애노테이션 규약상 있어야 할 뿐).
 *
 * null·빈 문자열은 통과시킨다 — 그건 @NotBlank의 일이다. 둘 다 에러를 내면 필드 에러가
 * 두 개 생기고, GlobalExceptionHandler가 그중 무엇을 고를지 순서가 보장되지 않는다.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LoginIdValidator.class)
public @interface ValidLoginId {

    String message() default "아이디 형식이 올바르지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
