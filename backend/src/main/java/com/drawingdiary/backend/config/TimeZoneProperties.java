package com.drawingdiary.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

/**
 * 시간대 설정. 두 값을 나눠 두는 이유는 <b>저장하는 시간대와 보여주는 시간대가 다르기</b> 때문이다.
 *
 * <h4>왜 필요한가</h4>
 * {@code diaries.created_at}은 {@code timestamp without time zone}이라 "몇 시"만 있고
 * "어느 시간대의 몇 시"인지가 없다. 값을 쓰는 주체가 둘이라(앱의 @CreationTimestamp,
 * 시드 스크립트의 PG {@code now()}) 두 주체의 시간대가 다르면 같은 컬럼에 서로 다른 기준의
 * 값이 섞인다. 실제로 그런 상태였다 — Railway는 JVM·DB 모두 UTC라 맞아떨어졌지만,
 * 로컬은 JVM이 Asia/Seoul이고 DB가 Etc/UTC라 9시간 어긋난 값이 한 컬럼에 공존했다.
 *
 * <h4>storage — 저장 기준</h4>
 * created_at에 적히는 벽시계의 시간대다. TimeZoneConfig가 JVM 기본 시간대를 이 값으로
 * 고정해서, 앱이 쓰든 DB의 now()가 쓰든 같은 기준이 되게 한다. 바꾸면 <b>이미 저장된 값의
 * 해석이 통째로 달라지므로</b> 운영 중에 건드리면 안 된다.
 *
 * <h4>display — 표시 기준</h4>
 * 서버가 날짜로 잘라 내려주는 화면(활동 잔디)이 쓰는 시간대. 잔디는 서버가 이미 "2026-09-11"
 * 같은 날짜로 묶어 보내므로 프론트가 시간대를 되돌릴 수 없다 — 그래서 이 값이 필요하다.
 * 반대로 createdAt을 그대로 내려주는 피드·일기 상세는 프론트가 로컬 시간대로 바꾸면 되므로
 * 이 설정과 무관하다.
 *
 * @param storage created_at이 적히는 시간대. 기본 UTC
 * @param display 날짜 단위로 묶어 내려줄 때 쓰는 시간대. 기본 Asia/Seoul
 */
@ConfigurationProperties(prefix = "app.time-zone")
public record TimeZoneProperties(
        ZoneId storage,
        ZoneId display
) {
}
