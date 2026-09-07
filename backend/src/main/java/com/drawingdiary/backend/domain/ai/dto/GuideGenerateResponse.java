package com.drawingdiary.backend.domain.ai.dto;

import java.util.List;

public record GuideGenerateResponse(List<GuideItemResponse> guides) {
}
