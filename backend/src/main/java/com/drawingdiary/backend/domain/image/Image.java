package com.drawingdiary.backend.domain.image;

import com.drawingdiary.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 업로드된 이미지 원본. 별도 저장소를 붙이기 전까지 바이너리를 DB에 그대로 담는다.
 *
 * data를 LAZY로 두지 않은 이유: 이 엔티티를 읽는 경로가 서빙 하나뿐이고, 그 경로는
 * 항상 바이너리를 필요로 한다. 나중에 목록 같은 메타데이터 전용 조회가 생기면 그때
 * 분리하는 편이 낫다.
 */
@Entity
@Table(name = "images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] data;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    /**
     * 업로더가 탈퇴해도 이미지는 남는다. 이미 발행된 일기가 그 이미지를 참조하고 있을 수
     * 있어서, 사람이 사라졌다고 그림까지 깨지면 안 되기 때문이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Image(byte[] data, String contentType, User uploader) {
        this.data = data;
        this.contentType = contentType;
        this.uploader = uploader;
    }
}
