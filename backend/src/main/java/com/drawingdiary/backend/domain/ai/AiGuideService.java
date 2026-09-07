package com.drawingdiary.backend.domain.ai;

import com.drawingdiary.backend.domain.ai.dto.GuideGenerateRequest;
import com.drawingdiary.backend.domain.ai.dto.GuideGenerateResponse;
import com.drawingdiary.backend.domain.ai.dto.GuideItemResponse;
import com.drawingdiary.backend.domain.ai.exception.AllGuidesFailedException;
import com.drawingdiary.backend.domain.image.ImageService;
import com.drawingdiary.backend.domain.room.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGuideService {

    private final AiServerClient aiServerClient;
    private final ImageService imageService;
    private final RoomService roomService;

    /**
     * 스타일 3종을 한꺼번에 생성한다. 순차로 부르면 스타일당 10초 남짓 × 3 = 30초가 넘고,
     * Render가 잠들어 있었다면 첫 호출만 50초가 더 붙는다.
     *
     * <p><b>병렬은 AI 호출까지만이다.</b> 이미지 저장은 요청 스레드로 돌아와서 순서대로 한다.
     * URL 생성이 요청 스레드에 묶인 ThreadLocal을 읽고, JPA 영속성 컨텍스트도 스레드 단위라
     * 작업 스레드 안에서 저장하면 둘 다 깨진다. 저장은 DB 쓰기 3번이라 순차로 해도 순식간이다.
     *
     * <p>가상 스레드를 쓰는 이유: 세 호출 모두 대부분의 시간을 응답 대기로 보내는 블로킹 IO다.
     * 플랫폼 스레드 풀을 두면 크기를 정해야 하고 그 값이 곧 동시 사용자 수 상한이 되는데,
     * 가상 스레드는 대기 중에 캐리어 스레드를 놓아주므로 그 고민이 없다. 다만 상한이 없다는 뜻이라
     * 동시 요청이 폭증하면 AI 서버로 나가는 호출도 같이 늘어난다 — 지금 규모에서는 문제되지
     * 않지만, 부하가 커지면 세마포어로 동시 호출 수를 막아야 한다.
     */
    public GuideGenerateResponse generate(Long userId, Long roomId, GuideGenerateRequest request) {
        roomService.requireEditableMember(userId, roomId);

        Map<GuideStyle, CompletableFuture<byte[]>> inFlight = new LinkedHashMap<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (GuideStyle style : GuideStyle.values()) {
                inFlight.put(style, CompletableFuture.supplyAsync(
                        () -> aiServerClient.generateGuide(request.text(), style), executor));
            }
        }
        // try-with-resources를 벗어나면서 executor.close()가 세 작업이 모두 끝날 때까지 기다린다.

        List<GuideItemResponse> guides = new ArrayList<>();
        int succeeded = 0;
        String lastError = null;

        for (Map.Entry<GuideStyle, CompletableFuture<byte[]>> entry : inFlight.entrySet()) {
            GuideStyle style = entry.getKey();
            try {
                byte[] image = entry.getValue().get();
                // AI 서버가 붙여준 image/png는 실제 형식과 다를 수 있어 바이트로 다시 판별한다.
                String url = imageService.storeGeneratedImage(userId, image, "image/png");
                guides.add(GuideItemResponse.success(style.getCode(), style.getDisplayName(), url));
                succeeded++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AllGuidesFailedException("생성 중 요청이 중단되었습니다.");
            } catch (ExecutionException | RuntimeException e) {
                Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                lastError = cause.getMessage();
                log.warn("선화 가이드 생성 실패 roomId={} style={}", roomId, style.getCode(), cause);
                guides.add(GuideItemResponse.failure(style.getCode(), style.getDisplayName(), lastError));
            }
        }

        // 하나라도 건졌으면 부분 성공으로 내려보낸다. 전부 실패면 보여줄 게 없으니 502.
        if (succeeded == 0) {
            throw new AllGuidesFailedException(lastError == null ? "" : lastError);
        }

        return new GuideGenerateResponse(guides);
    }
}
