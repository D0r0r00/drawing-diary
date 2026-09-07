package com.drawingdiary.backend.domain.image;

import com.drawingdiary.backend.domain.image.dto.ImageUploadResponse;
import com.drawingdiary.backend.domain.image.exception.EmptyImageException;
import com.drawingdiary.backend.domain.image.exception.ImageNotFoundException;
import com.drawingdiary.backend.domain.image.exception.UnsupportedImageTypeException;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE);

    /**
     * 서블릿 컨테이너의 multipart 제한(application.yml)과 같은 값. 컨테이너 쪽이 먼저
     * 걸러주지만, 이 서비스가 다른 경로에서 불릴 때를 대비해 여기서도 확인한다.
     */
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    /**
     * 반환 URL은 요청에서 만들어낸다. 로컬(http://localhost:8080)과 배포(https://...)가
     * 같은 코드로 동작해야 하고, 저장소를 S3로 옮기더라도 이 메서드의 반환 형태만 바꾸면
     * 프론트는 그대로 쓸 수 있다.
     *
     * 프록시 뒤에서 https를 잃지 않으려면 server.forward-headers-strategy 설정이 필요하다
     * (application.yml에 지정해둠). 그렇지 않으면 배포 환경에서 http URL이 만들어져
     * 브라우저가 mixed content로 막는다.
     */
    @Transactional
    public ImageUploadResponse upload(Long uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyImageException();
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new UnsupportedImageTypeException(contentType);
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new org.springframework.web.multipart.MaxUploadSizeExceededException(MAX_SIZE_BYTES);
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new UserNotFoundException(uploaderId));

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 읽지 못했습니다.", e);
        }

        Image saved = imageRepository.save(Image.builder()
                .data(data)
                .contentType(contentType.toLowerCase())
                .uploader(uploader)
                .build());

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/{imageId}")
                .buildAndExpand(saved.getId())
                .toUriString();

        return new ImageUploadResponse(url);
    }

    @Transactional(readOnly = true)
    public Image find(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));
    }
}
