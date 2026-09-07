package com.drawingdiary.backend.domain.ai;

import com.drawingdiary.backend.domain.ai.exception.AiFailureReason;
import com.drawingdiary.backend.domain.ai.exception.AiServerException;
import com.drawingdiary.backend.domain.ai.exception.FinalImageMissingException;
import com.drawingdiary.backend.domain.aiscore.AiScoreService;
import com.drawingdiary.backend.domain.aiscore.dto.AiScoreResponse;
import com.drawingdiary.backend.domain.aiscore.dto.AiScoreSaveRequest;
import com.drawingdiary.backend.domain.diary.Diary;
import com.drawingdiary.backend.domain.diary.DiaryRepository;
import com.drawingdiary.backend.domain.diary.DiaryCollaboratorRepository;
import com.drawingdiary.backend.domain.diary.exception.DiaryNotFoundException;
import com.drawingdiary.backend.domain.diary.exception.NotDiaryCollaboratorException;
import com.drawingdiary.backend.domain.image.Image;
import com.drawingdiary.backend.domain.image.ImageFormats;
import com.drawingdiary.backend.domain.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 일기의 완성 이미지를 AI 서버에 보내 점수를 받아오고, 저장은 기존 AiScoreService에 맡긴다.
 * 점수 공식(likeScore·totalScore)이 한 곳에만 있어야 수동 저장과 자동 산정이 어긋나지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AiDiaryScoreService {

    /**
     * 우리가 서빙하는 이미지 URL. 이 형태면 네트워크로 다시 받아오지 않고 DB에서 바로 꺼낸다
     * (자기 자신에게 HTTP 요청을 보내는 셈이라 느리고, 배포 환경에서는 자기 주소를 못 찾을 수도 있다).
     */
    private static final Pattern OWN_IMAGE_URL = Pattern.compile(".*/api/images/([0-9]+)/?$");

    private static final long MAX_DOWNLOAD_BYTES = 10L * 1024 * 1024;

    private final DiaryRepository diaryRepository;
    private final DiaryCollaboratorRepository diaryCollaboratorRepository;
    private final AiServerClient aiServerClient;
    private final AiScoreService aiScoreService;
    private final ImageService imageService;
    private final RestClient aiServerRestClient;

    /**
     * 트랜잭션을 열지 않는다. AI 호출이 수십 초 걸릴 수 있어서, 그동안 DB 커넥션을 붙들고
     * 있으면 커넥션 풀이 금방 마른다. 일기 조회·이미지 로딩·점수 저장은 각각 자기 트랜잭션에서
     * 짧게 끝난다.
     */
    public AiScoreResponse score(Long userId, Long diaryId) {
        DiarySnapshot snapshot = loadDiary(userId, diaryId);

        ImagePayload image = loadImage(snapshot.finalImgUrl(), diaryId);

        AiScoreResult result = aiServerClient.scoreDiary(
                snapshot.content(), image.data(), image.contentType(), image.filename());

        // 응답 검증(필드 누락·범위)은 AiServerClient가 저장 전에 끝낸다.
        AiScoreResponse saved = aiScoreService.save(userId, diaryId,
                new AiScoreSaveRequest(result.relevanceScore(), result.colorScore()));

        // AI 서버가 함께 주는 평가 코멘트. 응답 형식은 수동 저장과 같게 두고 DB에만 남긴다.
        aiScoreService.saveFeedback(diaryId, result.feedback());

        return saved;
    }

    /**
     * 여기에 @Transactional을 붙이지 않는다. 같은 클래스 안에서 부르면 프록시를 타지 않아
     * 어차피 적용되지 않고, 리포지토리 호출이 각자 짧은 트랜잭션으로 끝나 필요도 없다.
     * 뒤이어 수십 초짜리 AI 호출이 오기 때문에 여기서 트랜잭션을 여는 건 오히려 해롭다.
     */
    private DiarySnapshot loadDiary(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new DiaryNotFoundException(diaryId));

        // 점수는 일기에 남는 기록이라, 볼 수만 있는 사람이 아니라 협업자만 매길 수 있다.
        if (!diaryCollaboratorRepository.existsByDiaryIdAndUserId(diaryId, userId)) {
            throw new NotDiaryCollaboratorException(diaryId);
        }

        String finalImgUrl = diary.getFinalImgUrl();
        if (finalImgUrl == null || finalImgUrl.isBlank()) {
            throw new FinalImageMissingException(diaryId);
        }

        return new DiarySnapshot(diary.getContent(), finalImgUrl);
    }

    private ImagePayload loadImage(String finalImgUrl, Long diaryId) {
        Matcher matcher = OWN_IMAGE_URL.matcher(finalImgUrl);
        if (matcher.matches()) {
            Image image = imageService.find(Long.valueOf(matcher.group(1)));
            return new ImagePayload(image.getData(), image.getContentType(), filenameFor(image.getContentType()));
        }

        return downloadExternal(finalImgUrl, diaryId);
    }

    /**
     * 외부 URL에 올라간 완성 이미지. AI 서버용 RestClient를 재사용하는데, 타임아웃 성격이
     * 비슷하고(느린 외부 호출) 클라이언트를 하나 더 만들 이유가 없어서다. baseUrl이 걸려
     * 있지만 절대 URL을 넘기면 그대로 쓰인다.
     */
    private ImagePayload downloadExternal(String finalImgUrl, Long diaryId) {
        byte[] data;
        try {
            data = aiServerRestClient.get().uri(finalImgUrl).retrieve().body(byte[].class);
        } catch (ResourceAccessException e) {
            throw new AiServerException(AiFailureReason.UNAVAILABLE, "완성 이미지 다운로드 실패: " + finalImgUrl, e);
        } catch (RestClientException e) {
            throw new AiServerException(AiFailureReason.INVALID_RESPONSE, "완성 이미지 다운로드 실패: " + finalImgUrl, e);
        }

        if (data == null || data.length == 0) {
            throw new FinalImageMissingException(diaryId);
        }
        if (data.length > MAX_DOWNLOAD_BYTES) {
            throw new AiServerException(AiFailureReason.INVALID_RESPONSE,
                    "완성 이미지가 10MB를 넘습니다: " + finalImgUrl);
        }

        // 외부 서버의 Content-Type도 믿을 게 못 되므로 바이트로 판별한다.
        String contentType = ImageFormats.detect(data);
        if (contentType == null) {
            throw new AiServerException(AiFailureReason.INVALID_RESPONSE,
                    "완성 이미지가 png/jpeg/webp가 아닙니다: " + finalImgUrl);
        }

        return new ImagePayload(data, contentType, filenameFor(contentType));
    }

    /**
     * FastAPI의 UploadFile은 파일명이 있어야 파일 파트로 인식한다. 확장자는 Content-Type에 맞춘다.
     */
    private String filenameFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> "diary.png";
            case "image/webp" -> "diary.webp";
            default -> "diary.jpg";
        };
    }

    private record DiarySnapshot(String content, String finalImgUrl) {
    }

    private record ImagePayload(byte[] data, String contentType, String filename) {
    }
}
