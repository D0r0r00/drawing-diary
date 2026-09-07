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

    /**
     * AI 서버가 jpeg·png·webp를 받으므로 우리도 같은 집합을 허용한다. 예전에는 png·jpeg만
     * 열려 있어서, webp로 그린 그림은 업로드조차 안 되는데 AI 평가는 된다는 불일치가 있었다.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, ImageFormats.IMAGE_WEBP_VALUE);

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

        return new ImageUploadResponse(store(uploader, data, contentType.toLowerCase()));
    }

    /**
     * 업로드가 아닌 경로(AI 선화 가이드처럼 서버가 만들어낸 이미지)에서 쓰는 저장 진입점.
     * 저장과 URL 생성 규칙을 업로드와 공유하므로, 나중에 저장소를 S3로 옮겨도 한 곳만 고치면 된다.
     *
     * <p>선언된 Content-Type이 아니라 <b>실제 바이트</b>로 형식을 판별한다. AI 서버는
     * image/png라고 해놓고 JPEG를 주는데, 그대로 믿으면 브라우저에서 그림이 깨진다
     * (ImageFormats 주석 참고).
     *
     * <p><b>같은 트랜잭션·같은 요청 스레드에서 불러야 한다.</b> URL을 요청 컨텍스트에서
     * 만들기 때문에(ServletUriComponentsBuilder는 요청 스레드에 묶인 ThreadLocal을 읽는다)
     * 병렬 작업 스레드 안에서 부르면 URL을 만들지 못한다. AI 가이드 생성도 호출만 병렬로
     * 하고 저장은 요청 스레드로 돌아와서 한다.
     */
    @Transactional
    public String storeGeneratedImage(Long uploaderId, byte[] data, String declaredContentType) {
        if (data == null || data.length == 0) {
            throw new EmptyImageException();
        }

        String contentType = ImageFormats.detect(data);
        if (contentType == null) {
            throw new UnsupportedImageTypeException(declaredContentType);
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new UserNotFoundException(uploaderId));

        return store(uploader, data, contentType);
    }

    private String store(User uploader, byte[] data, String contentType) {
        Image saved = imageRepository.save(Image.builder()
                .data(data)
                .contentType(contentType)
                .uploader(uploader)
                .build());

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/{imageId}")
                .buildAndExpand(saved.getId())
                .toUriString();
    }

    @Transactional(readOnly = true)
    public Image find(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));
    }
}
