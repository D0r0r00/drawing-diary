package com.drawingdiary.backend.domain.comment;

import com.drawingdiary.backend.domain.comment.dto.CommentCreateRequest;
import com.drawingdiary.backend.domain.comment.dto.CommentCreateResponse;
import com.drawingdiary.backend.domain.comment.dto.CommentDeleteResponse;
import com.drawingdiary.backend.domain.comment.dto.CommentResponse;
import com.drawingdiary.backend.domain.comment.exception.CommentNotFoundException;
import com.drawingdiary.backend.domain.comment.exception.NotCommentAuthorException;
import com.drawingdiary.backend.domain.diary.Diary;
import com.drawingdiary.backend.domain.diary.DiaryService;
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

        return new CommentCreateResponse(comment.getId(), comment.getContent(), comment.getCreatedAt());
    }

    /**
     * 일기 협업자나 방장이 아니라 <b>댓글 작성자 본인</b>만 지울 수 있다.
     * comment.user는 LAZY 프록시지만 id만 읽으므로 User를 실제로 조회하지 않는다.
     */
    @Transactional
    public CommentDeleteResponse delete(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new NotCommentAuthorException(commentId);
        }

        commentRepository.delete(comment);

        return new CommentDeleteResponse("댓글이 삭제되었습니다");
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
