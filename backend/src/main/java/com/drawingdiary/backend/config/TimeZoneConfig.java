package com.drawingdiary.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * TimeZoneProperties를 빈으로 올리고, 실제로 고정된 시간대를 한 줄 남긴다.
 *
 * <p>고정하는 일 자체는 {@link TimeZoneEnvironmentPostProcessor}가 한다 — 여기(@PostConstruct)서
 * 하면 Hibernate가 시간대를 붙잡은 뒤라 늦다. 자세한 사정은 그 클래스 주석 참고.
 *
 * <p>기동 로그에 실제 값을 남기는 이유는, 저장 기준이 어긋나면 잔디가 하루씩 밀리는데
 * 그때 가장 먼저 확인할 것이 "이 서버가 어느 시간대로 저장하고 있는가"이기 때문이다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TimeZoneProperties.class)
@RequiredArgsConstructor
public class TimeZoneConfig {

    private final TimeZoneProperties timeZoneProperties;

    @PostConstruct
    void logTimeZone() {
        log.info("시간대 — 저장 {} (JVM 기본 {}), 표시 {}",
                timeZoneProperties.storage(), TimeZone.getDefault().toZoneId(), timeZoneProperties.display());
    }
}
