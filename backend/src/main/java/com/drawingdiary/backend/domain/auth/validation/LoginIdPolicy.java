package com.drawingdiary.backend.domain.auth.validation;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 아이디 규칙을 한곳에 모은다. 가입 검증(LoginIdValidator), 가입 저장, 로그인 조회가 전부
 * 이 클래스를 거쳐야 "Test로 가입했는데 test로 로그인이 안 된다" 같은 어긋남이 생기지 않는다.
 *
 * <h4>대소문자를 구분하지 않는 방법: 저장할 때 소문자로 만든다</h4>
 * 입력은 대문자를 섞어 받아도 되지만, 검증·저장·조회는 전부 {@link #normalize} 결과로 한다.
 * DB에는 소문자만 들어가므로 login_id의 평범한 UNIQUE 제약이 곧 "대소문자 무시 중복 금지"가
 * 되고, lower() 함수 인덱스 같은 게 필요 없다. DB의 CHECK 제약(ck_users_login_id_format)도
 * 소문자 패턴이라, 앱을 거치지 않는 쓰기도 이 규칙을 벗어나지 못한다.
 */
public final class LoginIdPolicy {

    public static final int MIN_LENGTH = 4;

    public static final int MAX_LENGTH = 20;

    private LoginIdPolicy() {
    }

    /**
     * 비교·저장용 형태. 공백은 건드리지 않는다 — 앞뒤 공백을 조용히 잘라주면 사용자가 친 것과
     * 다른 아이디로 가입되는데, 그보다 "쓸 수 없는 문자"로 400을 받는 편이 덜 헷갈린다.
     */
    public static String normalize(String loginId) {
        return loginId == null ? null : loginId.toLowerCase(Locale.ROOT);
    }

    /**
     * 규칙 위반 사유. 어긴 규칙이 여러 개면 사용자가 가장 먼저 고쳐야 할 것 하나만 알려준다:
     * 쓸 수 없는 문자 → 첫 글자 → 길이 순. 문자를 먼저 보는 이유는 "한글아이디"에
     * "첫 글자는 영문이어야" 같은 엉뚱한 안내가 나가지 않게 하기 위해서다.
     *
     * @return 위반이 없으면 empty
     */
    public static Optional<String> violation(String loginId) {
        String normalized = normalize(loginId);

        String invalidChars = normalized.chars()
                .filter(ch -> !isAllowed(ch))
                .distinct()
                .mapToObj(ch -> describe(ch))
                .collect(Collectors.joining(", "));
        if (!invalidChars.isEmpty()) {
            return Optional.of("아이디는 영문, 숫자, 밑줄(_)만 쓸 수 있습니다. 쓸 수 없는 문자: " + invalidChars);
        }

        if (!isLetter(normalized.charAt(0))) {
            return Optional.of("아이디의 첫 글자는 영문이어야 합니다.");
        }

        int length = normalized.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return Optional.of("아이디는 " + MIN_LENGTH + "~" + MAX_LENGTH + "자여야 합니다. (현재 " + length + "자)");
        }

        return Optional.empty();
    }

    private static boolean isAllowed(int ch) {
        return isLetter(ch) || (ch >= '0' && ch <= '9') || ch == '_';
    }

    /**
     * 소문자 a~z만. Character.isLetter는 한글·악센트 문자까지 글자로 치므로 쓰면 안 된다.
     */
    private static boolean isLetter(int ch) {
        return ch >= 'a' && ch <= 'z';
    }

    private static String describe(int ch) {
        return Character.isWhitespace(ch) ? "공백" : "'" + new String(Character.toChars(ch)) + "'";
    }
}
