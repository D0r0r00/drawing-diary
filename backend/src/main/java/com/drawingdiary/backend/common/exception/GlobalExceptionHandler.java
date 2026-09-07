package com.drawingdiary.backend.common.exception;

import com.drawingdiary.backend.domain.auth.exception.AuthTokenException;
import com.drawingdiary.backend.domain.auth.exception.DuplicateEmailException;
import com.drawingdiary.backend.domain.auth.exception.InvalidCredentialsException;
import com.drawingdiary.backend.domain.category.exception.CategoryNotFoundException;
import com.drawingdiary.backend.domain.category.exception.DuplicateCategoryNameException;
import com.drawingdiary.backend.domain.category.exception.NotCategoryOwnerException;
import com.drawingdiary.backend.domain.comment.exception.CommentNotFoundException;
import com.drawingdiary.backend.domain.comment.exception.NotCommentAuthorException;
import com.drawingdiary.backend.domain.diary.exception.DiaryAccessDeniedException;
import com.drawingdiary.backend.domain.diary.exception.DiaryNotFoundException;
import com.drawingdiary.backend.domain.diary.exception.NotDiaryCollaboratorException;
import com.drawingdiary.backend.domain.follow.exception.AlreadyFollowingException;
import com.drawingdiary.backend.domain.follow.exception.SelfFollowException;
import com.drawingdiary.backend.domain.notification.exception.NotNotificationReceiverException;
import com.drawingdiary.backend.domain.notification.exception.NotificationNotFoundException;
import com.drawingdiary.backend.domain.room.exception.NotRoomMemberException;
import com.drawingdiary.backend.domain.room.exception.NotRoomOwnerException;
import com.drawingdiary.backend.domain.room.exception.OwnerCannotLeaveRoomException;
import com.drawingdiary.backend.domain.room.exception.RoomAlreadyFinishedException;
import com.drawingdiary.backend.domain.room.exception.RoomInviteNotFoundException;
import com.drawingdiary.backend.domain.room.exception.RoomNotFoundException;
import com.drawingdiary.backend.domain.user.exception.DuplicateNicknameException;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 토큰 재발급처럼 컨트롤러까지 도달한 인증 실패. 시큐리티 필터에서 끊긴 요청은
     * JwtAuthenticationEntryPoint가 처리하지만, 양쪽 응답 형태가 같아야 프론트가
     * 인증 에러를 한 곳에서 처리할 수 있으므로 여기서도 {code, message}로 내려준다.
     */
    @ExceptionHandler(AuthTokenException.class)
    public ResponseEntity<AuthErrorResponse> handleAuthToken(AuthTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(DuplicateNicknameException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AlreadyFollowingException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyFollowing(AlreadyFollowingException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ErrorResponse> handleSelfFollow(SelfFollowException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(DuplicateCategoryNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCategoryName(DuplicateCategoryNameException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotCategoryOwnerException.class)
    public ResponseEntity<ErrorResponse> handleNotCategoryOwner(NotCategoryOwnerException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotRoomMemberException.class)
    public ResponseEntity<ErrorResponse> handleNotRoomMember(NotRoomMemberException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotRoomOwnerException.class)
    public ResponseEntity<ErrorResponse> handleNotRoomOwner(NotRoomOwnerException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * 403이지 404가 아니다: 방은 실제로 존재하고, 초대가 없어 참여 권한이 없을 뿐이다.
     */
    @ExceptionHandler(RoomInviteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomInviteNotFound(RoomInviteNotFoundException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(OwnerCannotLeaveRoomException.class)
    public ResponseEntity<ErrorResponse> handleOwnerCannotLeaveRoom(OwnerCannotLeaveRoomException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(RoomAlreadyFinishedException.class)
    public ResponseEntity<ErrorResponse> handleRoomAlreadyFinished(RoomAlreadyFinishedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(DiaryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDiaryNotFound(DiaryNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(DiaryAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDiaryAccessDenied(DiaryAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotDiaryCollaboratorException.class)
    public ResponseEntity<ErrorResponse> handleNotDiaryCollaborator(NotDiaryCollaboratorException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotCommentAuthorException.class)
    public ResponseEntity<ErrorResponse> handleNotCommentAuthor(NotCommentAuthorException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(NotificationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotNotificationReceiverException.class)
    public ResponseEntity<ErrorResponse> handleNotNotificationReceiver(NotNotificationReceiverException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }
}
