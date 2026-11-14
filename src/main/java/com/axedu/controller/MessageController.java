package com.axedu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.axedu.common.response.ApiResponse;
import com.axedu.common.response.PageMeta;
import com.axedu.common.security.AuthUser;
import com.axedu.common.security.CurrentUser;
import com.axedu.config.WebConfig;
import com.axedu.dto.chat.MessageResponse;
import com.axedu.dto.chat.ReadRequest;
import com.axedu.dto.chat.SendMessageRequest;
import com.axedu.service.MessageService;

import lombok.RequiredArgsConstructor;

/** 메시지 (계약서 3.7, 3.8, 3.10) */
@RestController
@RequestMapping(WebConfig.API_BASE + "/chat/rooms/{roomId}")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 항상 최신순 + meta.hasMore */
    @GetMapping("/messages")
    public ApiResponse<List<MessageResponse>> list(@CurrentUser AuthUser user, @PathVariable String roomId,
                                                   @RequestParam(required = false) String before,
                                                   @RequestParam(required = false) String after,
                                                   @RequestParam(required = false) Integer limit) {
        MessageService.MessagePage page = messageService.list(user, roomId, before, after, limit);
        return ApiResponse.ok(page.items(), PageMeta.cursor(page.nextCursor(), page.hasMore()));
    }

    /** 새로 저장하면 201, 같은 clientMessageId 재전송이면 기존 메시지를 200 으로 */
    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> send(@CurrentUser AuthUser user, @PathVariable String roomId,
                                                             @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                                             @RequestBody SendMessageRequest req) {
        MessageService.SendResult result = messageService.send(user, roomId, req, idempotencyKey);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(ApiResponse.ok(result.message()));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> read(@CurrentUser AuthUser user, @PathVariable String roomId,
                                     @RequestBody(required = false) ReadRequest req) {
        messageService.markRead(user, roomId, req);
        return ResponseEntity.noContent().build();
    }
}
