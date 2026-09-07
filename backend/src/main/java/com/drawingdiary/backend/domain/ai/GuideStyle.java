package com.drawingdiary.backend.domain.ai;

import java.util.Arrays;

/**
 * AI 서버가 받는 선화 스타일. 서버 쪽 값이 문자열 "1"·"2"·"3"이라 enum 이름 대신
 * code를 그대로 실어 보내고, 응답에도 code와 한글 이름을 함께 내려준다.
 */
public enum GuideStyle {

    WEBTOON("1", "웹툰형"),
    COLORING_BOOK("2", "컬러링북형"),
    HIGH_QUALITY_ANIMATION("3", "고퀄리티 애니메이션형");

    private final String code;
    private final String displayName;

    GuideStyle(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GuideStyle fromCode(String code) {
        return Arrays.stream(values())
                .filter(style -> style.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 스타일: " + code));
    }
}
