package com.drawingdiary.backend.domain.image;

import com.drawingdiary.backend.domain.image.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping
    public ResponseEntity<ImageUploadResponse> upload(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageService.upload(currentUserId(authentication), file));
    }

    /**
     * 인증 없이 열어둔 경로(SecurityConfig 참고). &lt;img src&gt;는 Authorization 헤더를
     * 붙일 수 없으므로 토큰을 요구하면 이미지가 아예 뜨지 않는다.
     *
     * 한 번 저장된 이미지는 내용이 바뀌지 않고 수정 API도 없으므로 immutable로 캐시한다.
     * 그림이 바뀌면 새 업로드 = 새 id = 새 URL이라 캐시가 낡을 일이 없다.
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> serve(@PathVariable Long imageId) {
        Image image = imageService.find(imageId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.getData());
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
