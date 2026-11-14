package com.axedu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.axedu.common.response.ApiResponse;
import com.axedu.common.security.AuthUser;
import com.axedu.common.security.CurrentUser;
import com.axedu.config.WebConfig;
import com.axedu.dto.chat.ChatSettingDto;
import com.axedu.dto.user.UserSummary;
import com.axedu.service.ChatSettingService;
import com.axedu.service.UserService;

import lombok.RequiredArgsConstructor;

/** 로그인 사용자 (계약서 3.11, 6장 1번) */
@RestController
@RequestMapping(WebConfig.API_BASE + "/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final ChatSettingService chatSettingService;

    /**
     * GET /me — 추가 API (계약서 6장 1번 권장안 b). 학부모 childStudentIds 를 서버에서 내려 준다.
     * 프론트 getMe 가 토큰 디코드 대신(또는 보충으로) 이 API 를 부르도록 바꾸면 된다.
     */
    @GetMapping
    public ApiResponse<UserSummary> me(@CurrentUser AuthUser user) {
        return ApiResponse.ok(userService.me(user));
    }

    @GetMapping("/chat-settings")
    public ApiResponse<ChatSettingDto> getChatSettings(@CurrentUser AuthUser user) {
        return ApiResponse.ok(chatSettingService.get(user));
    }

    @PutMapping("/chat-settings")
    public ApiResponse<ChatSettingDto> saveChatSettings(@CurrentUser AuthUser user, @RequestBody ChatSettingDto req) {
        return ApiResponse.ok(chatSettingService.save(user, req));
    }
}
