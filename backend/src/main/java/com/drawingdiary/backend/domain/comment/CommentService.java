package com.drawingdiary.backend.domain.comment;

import com.drawingdiary.backend.domain.comment.dto.CommentCreateRequest;
import com.drawingdiary.backend.domain.comment.dto.CommentCreateResponse;
import com.drawingdiary.backend.domain.comment.dto.CommentDeleteResponse;
import com.drawingdiary.backend.domain.comment.dto.CommentResponse;
import com.drawingdiary.backend.domain.comment.dto.CommentUpdateRequest;
import com.drawingdiary.backend.domain.comment.dto.CommentUpdateResponse;
import com.drawingdiary.backend.domain.comment.exception.CommentNotFoundException;
import com.drawingdiary.backend.domain.comment.exception.NotCommentAuthorException;
import com.drawingdiary.backend.domain.diary.Diary;
import com.drawingdiary.backend.domain.diary.DiaryService;
import com.drawingdiary.backend.domain.notification.NotificationService;
import com.drawingdiary.backend.domain.notification.NotificationType;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final DiaryService diaryService;
    private final NotificationService notificationService;

    /**
     * 일기를 볼 수 없는 사람은 댓글도 볼 수 없다 — 권한 판정은 DiaryService에 맡긴다.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> findAll(Long userId, Long diaryId) {
        diaryService.getReadableDiaryOrThrow(userId, diaryId);

        return commentRepository.findWithUserByDiaryId(diaryId).stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getNickname(),
                        comment.getContent(),
                        comment.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public CommentCreateResponse create(Long userId, Long diaryId, CommentCreateRequest request) {
        Diary diary = diaryService.getReadableDiaryOrThrow(userId, diaryId);
        User user = getUserOrThrow(userId);

        // 응답에 담을 id와 createdAt이 INSERT 시점에 채워지므로, 커밋을 기다리지 않고 flush한다.
        Comment comment = commentRepository.saveAndFlush(Comment.builder()
                .diary(diary)
                .user(user)
                .content(request.content())
                .build());

        // 받는 사람은 일기 작성자(= 방장). 본인 일기에 본인이 단 댓글이면 notify가 걸러낸다.
        notificationService.notify(
                diaryService.findAuthorId(diaryId), userId, NotificationType.COMMENT, diaryId);

        return new CommentCreateResponse(comment.getId(), comment.getContent(), comment.getCreatedAt());
    }

    /**
     * 수정도 삭제와 같이 <b>댓글 작성자 본인</b>만 할 수 있다. 일기 주인이라도 남의 댓글
     * 내용을 바꿀 수는 없다.
     *
     * <p>작성 때와 달리 일기 조회 권한을 다시 보지 않는다. 삭제와 같은 판단인데, 이미 남긴
     * 내 글을 고치는 일이라 나중에 일기가 비공개로 바뀌었다고 손댈 수 없게 되면 곤란하다.
     *
     * <p>알림을 보내지 않는 것도 의도다. 수정할 때마다 알림이 다시 가면 한 댓글로 알림을
     * 몇 번이든 만들 수 있다.
     */
    @Transactional
    public CommentUpdateResponse update(Long userId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getCommentOrThrow(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new NotCommentAuthorException(commentId);
        }

        comment.updateContent(request.content());

        // @UpdateTimestamp는 flush 시점에 채워지므로, 응답에 방금 수정한 시각을 담으려면
        // 커밋을 기다리지 않고 여기서 flush해야 한다(작성 응답의 saveAndFlush와 같은 이유).
        commentRepository.flush();

        return new CommentUpdateResponse(comment.getId(), comment.getContent(), comment.getUpdatedAt());
    }

    /**
     * 일기 협업자나 방장이 아니라 <b>댓글 작성자 본인</b>만 지울 수 있다.
     * comment.user는 LAZY 프록시지만 id만 읽으므로 User를 실제로 조회하지 않는다.
     */
    @Transactional
    public CommentDeleteResponse delete(Long userId, Long commentId) {
        Comment comment = getCommentOrThrow(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new NotCommentAuthorException(commentId);
        }

        commentRepository.delete(comment);

        return new CommentDeleteResponse("댓글이 삭제되었습니다");
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
