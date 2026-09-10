package com.drawingdiary.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.TimeZone;

/**
 * JVM 기본 시간대를 {@code app.time-zone.storage}로 고정한다.
 *
 * <h4>왜 @PostConstruct가 아니라 EnvironmentPostProcessor인가</h4>
 * 처음에는 @Configuration 빈의 @PostConstruct에서 고정했는데 <b>동작하지 않았다.</b>
 * Hibernate가 SessionFactory를 만들 때 {@code Clock.systemDefaultZone()}을 붙잡아 두는데,
 * 그 시점이 일반 빈의 @PostConstruct보다 앞선다. 그래서 로그에는 "UTC로 고정"이 찍히는데
 * {@code @CreationTimestamp}는 계속 예전 시간대(Asia/Seoul)로 값을 쓰는, 설정과 실제가
 * 어긋난 상태가 됐다. 그 상태가 고치기 전보다 더 나쁘다 — 저장은 KST인데 읽을 때 UTC로
 * 해석하므로 저녁에 쓴 일기가 다음날 칸으로 밀린다.
 *
 * <p>EnvironmentPostProcessor는 <b>애플리케이션 컨텍스트가 만들어지기 전에</b> 실행되므로
 * Hibernate를 포함해 무엇보다 먼저 시간대를 잡는다. 이 클래스는 spring.factories에
 * 등록되어 있어야 동작한다.
 *
 * <h4>기본값이 UTC인 이유</h4>
 * 임의로 고른 값이 아니라 이미 저장된 데이터에 맞춘 것이다. 운영 DB(Railway)는 JVM과
 * PostgreSQL이 모두 UTC라 기존 행이 전부 UTC이고, 시드 스크립트가 쓰는 PG {@code now()}도
 * UTC다. 즉 운영의 저장 동작은 그대로 두고 <b>원래 그랬던 것을 명시</b>할 뿐이다.
 * 로컬은 JVM만 Asia/Seoul이었어서 앱이 쓴 행과 시드가 쓴 행이 9시간 어긋나 있었는데,
 * 이제 양쪽 다 UTC로 통일된다.
 *
 * <p>표시 시간대({@code app.time-zone.display})는 여기서 건드리지 않는다. 저장은 UTC로
 * 두고 보여줄 때만 KST로 바꾸는 것이 이 설계의 요점이다.
 */
public class TimeZoneEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String STORAGE_ZONE_PROPERTY = "app.time-zone.storage";

    private static final String DEFAULT_STORAGE_ZONE = "UTC";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // application.yml이 아직 안 읽혔을 수도 있어(프로세서 실행 순서는 보장되지 않는다)
        // 값이 없으면 기본값으로 간다. yml의 기본값과 같은 값이라 결과는 어느 쪽이든 같다.
        String zone = environment.getProperty(STORAGE_ZONE_PROPERTY, DEFAULT_STORAGE_ZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
    }
}
