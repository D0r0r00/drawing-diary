package com.drawingdiary.backend.domain.room;

import com.drawingdiary.backend.domain.room.dto.RoomCreateResponse;
import com.drawingdiary.backend.domain.room.dto.RoomInviteRequest;
import com.drawingdiary.backend.domain.room.dto.RoomResponse;
import com.drawingdiary.backend.domain.room.dto.RoomSubmitRequest;
import com.drawingdiary.backend.domain.room.dto.RoomSubmitResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomCreateResponse> create(Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(currentUserId(authentication)));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> find(Authentication authentication, @PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.find(currentUserId(authentication), roomId));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long roomId) {
        roomService.delete(currentUserId(authentication), roomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> invite(
            Authentication authentication,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomInviteRequest request
    ) {
        roomService.invite(currentUserId(authentication), roomId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> join(Authentication authentication, @PathVariable Long roomId) {
        roomService.join(currentUserId(authentication), roomId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<Void> leave(Authentication authentication, @PathVariable Long roomId) {
        roomService.leave(currentUserId(authentication), roomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/submit")
    public ResponseEntity<RoomSubmitResponse> submit(
            Authentication authentication,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomSubmitRequest request
    ) {
        return ResponseEntity.ok(roomService.submit(currentUserId(authentication), roomId, request));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
