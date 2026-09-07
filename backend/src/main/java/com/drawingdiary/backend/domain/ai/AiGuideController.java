package com.drawingdiary.backend.domain.ai;

import com.drawingdiary.backend.domain.ai.dto.GuideGenerateRequest;
import com.drawingdiary.backend.domain.ai.dto.GuideGenerateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/ai-guide")
@RequiredArgsConstructor
public class AiGuideController {

    private final AiGuideService aiGuideService;

    @PostMapping
    public ResponseEntity<GuideGenerateResponse> generate(
            Authentication authentication,
            @PathVariable Long roomId,
            @Valid @RequestBody GuideGenerateRequest request
    ) {
        return ResponseEntity.ok(
                aiGuideService.generate((Long) authentication.getPrincipal(), roomId, request));
    }
}
