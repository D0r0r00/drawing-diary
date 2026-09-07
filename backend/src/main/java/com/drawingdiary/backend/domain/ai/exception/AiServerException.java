package com.drawingdiary.backend.domain.ai.exception;

public class AiServerException extends RuntimeException {

    private final AiFailureReason reason;

    public AiServerException(AiFailureReason reason, String detail) {
        super(reason.getMessage() + (detail == null || detail.isBlank() ? "" : " (" + detail + ")"));
        this.reason = reason;
    }

    public AiServerException(AiFailureReason reason, String detail, Throwable cause) {
        super(reason.getMessage() + (detail == null || detail.isBlank() ? "" : " (" + detail + ")"), cause);
        this.reason = reason;
    }

    public AiFailureReason getReason() {
        return reason;
    }
}
