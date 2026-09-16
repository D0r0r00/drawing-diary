package com.drawingdiary.backend.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
/*
 * login_id의 UNIQUE 제약은 이름을 명시한다. 컬럼에 unique = true로 두면 Hibernate가 해시 이름
 * (uk6dot... 같은)으로 제약을 만드는데, 마이그레이션(infra/migrate-login-id.sql)이 만든
 * uk_users_login_id와 이름이 달라 기동할 때마다 같은 컬럼에 UNIQUE 인덱스가 하나 더 붙는다.
 */
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"))
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    /**
     * 로그인 식별자. 항상 소문자로 저장된다(LoginIdPolicy.normalize) — 그래서 평범한 UNIQUE
     * 제약만으로 대소문자를 무시한 중복 금지가 된다.
     */
    @Column(name = "login_id", nullable = false, length = 20)
    private String loginId;

    /**
     * 로그인에는 쓰지 않는 연락처. 선택 입력이고 중복을 허용한다(예전에는 로그인 키라 UNIQUE였다).
     * 이 컬럼으로 사람을 찾으면 안 된다 — 같은 이메일을 쓰는 계정이 여럿일 수 있다.
     */
    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(name = "profile_img_url", length = 500)
    private String profileImageUrl;

    @Column(length = 500)
    private String bio;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    public User(String loginId, String email, String password, String nickname) {
        this.loginId = loginId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    /**
     * 부분 수정. null인 인자는 "보내지 않았다"는 뜻이므로 기존 값을 유지한다.
     * 값을 비우려면 빈 문자열을 보낸다 — 그래야 "생략"과 "지우기"가 구분된다.
     */
    public void applyProfileUpdate(String nickname, String profileImageUrl, String bio) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (bio != null) {
            this.bio = bio;
        }
    }
}
