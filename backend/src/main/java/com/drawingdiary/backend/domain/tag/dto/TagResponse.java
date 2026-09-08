package com.drawingdiary.backend.domain.tag.dto;

/**
 * 태그 검색·일기 조회가 공유하는 형태. 어느 경로에서 받든 같은 tagId를 쓰므로
 * 프론트가 검색 결과와 일기에 달린 태그를 같은 것으로 취급할 수 있다.
 */
public record TagResponse(
        Long tagId,
        String name
) {
}
