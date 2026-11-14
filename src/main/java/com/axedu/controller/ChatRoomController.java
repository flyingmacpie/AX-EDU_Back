package com.axedu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.axedu.common.response.ApiResponse;
import com.axedu.common.response.PageMeta;
import com.axedu.common.security.AuthUser;
import com.axedu.common.security.CurrentUser;
import com.axedu.config.WebConfig;
import com.axedu.dto.chat.ChatRoomDetailResponse;
import com.axedu.dto.chat.ChatRoomResponse;
import com.axedu.dto.chat.CreateRoomRequest;
import com.axedu.dto.chat.UpdateRoomSettingsRequest;
import com.axedu.entity.enums.RoomType;
import com.axedu.service.ChatRoomService;

import lombok.RequiredArgsConstructor;

/** 채팅방 (계약서 3.1, 3.3~3.6) */
@RestController
@RequestMapping(WebConfig.API_BASE + "/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public ApiResponse<List<ChatRoomResponse>> list(@CurrentUser AuthUser user,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(required = false) RoomType type,
                                                    @RequestParam(required = false) String cursor,
                                                    @RequestParam(required = false) Integer limit) {
        ChatRoomService.RoomPage page = chatRoomService.list(user, q, type, cursor, limit);
        return ApiResponse.ok(page.items(), PageMeta.cursor(page.nextCursor()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> create(@CurrentUser AuthUser user, @RequestBody CreateRoomRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(chatRoomService.create(user, req == null ? null : req.counterpartId())));
    }

    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomDetailResponse> detail(@CurrentUser AuthUser user, @PathVariable String roomId) {
        return ApiResponse.ok(chatRoomService.detail(user, roomId));
    }

    @PatchMapping("/{roomId}/settings")
    public ApiResponse<ChatRoomResponse> updateSettings(@CurrentUser AuthUser user, @PathVariable String roomId,
                                                        @RequestBody UpdateRoomSettingsRequest req) {
        return ApiResponse.ok(chatRoomService.updateSettings(user, roomId, req));
    }

    @DeleteMapping("/{roomId}/members/me")
    public ResponseEntity<Void> leave(@CurrentUser AuthUser user, @PathVariable String roomId) {
        chatRoomService.leave(user, roomId);
        return ResponseEntity.noContent().build();
    }
}
