package com.drawingdiary.backend.domain.ai;

import com.drawingdiary.backend.domain.ai.exception.AiFailureReason;
import com.drawingdiary.backend.domain.ai.exception.AiServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * AI 서버(https://drawing-diary.onrender.com) 호출을 한곳에 모은다. 호출부가 RestClient의
 * 예외 종류를 일일이 구분하지 않도록, 여기서 전부 AiServerException + AiFailureReason으로
 * 번역해서 던진다.
 */
@Component
@RequiredArgsConstructor
public class AiServerClient {

    private static final String GUIDE_PATH = "/api/ai/guide";

    private static final String SCORE_PATH = "/api/ai/score";

    private final RestClient aiServerRestClient;

    /**
     * 선화 가이드 한 장. 응답은 이미지 바이너리다.
     *
     * <p><b>Content-Type을 믿지 않는다.</b> AI 서버는 헤더에 image/png를 붙여 보내지만
     * 실제 바이트는 JPEG(FFD8FF…)다. 선언된 값을 그대로 저장하면 우리 이미지 서빙이
     * nosniff와 함께 image/png로 내려주게 되고, 브라우저가 JPEG를 PNG로 디코딩하려다
     * 실패해 그림이 깨진다. 그래서 바이트만 돌려주고 형식 판별은 ImageFormats에 맡긴다.
     */
    public byte[] generateGuide(String text, GuideStyle style) {
        byte[] image = call(
                () -> aiServerRestClient.post()
                        .uri(GUIDE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("text", text, "style", style.getCode()))
                        .retrieve()
                        .body(byte[].class),
                "style=" + style.getCode());

        if (image == null || image.length == 0) {
            throw new AiServerException(AiFailureReason.INVALID_RESPONSE,
                    "style=" + style.getCode() + ", 본문이 비어 있음");
        }
        return image;
    }

    /**
     * 일기 텍스트와 완성 이미지를 보내 점수를 받는다.
     *
     * 요청은 multipart(text + image) 두 파트뿐이다 — 좋아요 수 같은 추가 파라미터는
     * 받지 않는다. 좋아요 점수와 총점은 백엔드가 계산한다.
     */
    public AiScoreResult scoreDiary(String text, byte[] image, String contentType, String filename) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("text", text);
        // filename이 없으면 스프링이 이 파트를 파일이 아닌 일반 필드로 보내고,
        // FastAPI(UploadFile)는 그걸 이미지로 받아주지 않아 422가 난다.
        body.part("image", new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(MediaType.parseMediaType(contentType));

        AiScoreResult result = call(
                () -> aiServerRestClient.post()
                        .uri(SCORE_PATH)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body.build())
                        .retrieve()
                        .body(AiScoreResult.class),
                "score");

        return validate(result);
    }

    /**
     * 200을 받았다고 응답이 쓸 만한 건 아니다. 필드가 빠지거나 범위를 벗어난 값이 오면
     * 그대로 저장했을 때 0점짜리 가짜 점수가 랭킹에 들어가므로, 저장 전에 여기서 끊는다.
     */
    private AiScoreResult validate(AiScoreResult result) {
        if (result == null) {
            throw new AiServerException(AiFailureReason.INVALID_RESPONSE, "점수 응답이 비어 있음");
        }
        if (result.relevanceScore() == null || result.colorScore() == null) {
            throw new AiServerException(AiFailureReason.INVALID_RESPONSE,
                    "필수 점수 누락: relevanceScore=" + result.relevanceScore()
                            + ", colorScore=" + result.colorScore());
        }
        if (outOfRange(result.relevanceScore()) || outOfRange(result.colorScore())) {
            throw new AiServerException(AiFailureReason.INVALID_RESPONSE,
                    "점수가 0~100을 벗어남: relevanceScore=" + result.relevanceScore()
                            + ", colorScore=" + result.colorScore());
        }
        return result;
    }

    private boolean outOfRange(int score) {
        return score < 0 || score > 100;
    }

    /**
     * RestClient가 던지는 예외를 실패 원인별로 갈라준다. 호출부와 로그에서 "타임아웃인지,
     * 서버가 죽었는지, 응답이 이상한지"가 구분돼야 대응이 갈린다.
     *
     * <p><b>예외 타입만 보고 판단하면 안 된다.</b> read timeout이 항상 ResourceAccessException으로
     * 오는 게 아니라, 본문을 읽는 도중에 끊기면 메시지 컨버터가 감싼 RestClientException으로
     * 올라온다. 타입으로만 갈랐더니 타임아웃이 "응답 형식 오류"로 보고돼서, 원인 사슬에
     * SocketTimeoutException이 있는지를 먼저 본다.
     */
    private <T> T call(RestCall<T> restCall, String context) {
        try {
            return restCall.execute();
        } catch (RestClientResponseException e) {
            // 응답은 왔는데 상태 코드가 실패. 4xx면 우리 요청이 스펙과 안 맞는 것이다.
            AiFailureReason reason = e.getStatusCode().is4xxClientError()
                    ? AiFailureReason.BAD_REQUEST
                    : AiFailureReason.UNAVAILABLE;
            throw new AiServerException(reason, context + ", status=" + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException(classify(e), context, e);
        }
    }

    private AiFailureReason classify(RestClientException e) {
        if (hasCause(e, SocketTimeoutException.class)) {
            return AiFailureReason.TIMEOUT;
        }
        if (hasCause(e, ConnectException.class) || hasCause(e, UnknownHostException.class)
                || e instanceof ResourceAccessException) {
            return AiFailureReason.UNAVAILABLE;
        }
        // 통신은 됐는데 우리가 읽을 수 없는 응답이 온 경우(JSON 파싱 실패 등).
        return AiFailureReason.INVALID_RESPONSE;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface RestCall<T> {
        T execute();
    }
}
