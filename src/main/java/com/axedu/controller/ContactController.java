package com.axedu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.axedu.common.response.ApiResponse;
import com.axedu.common.security.AuthUser;
import com.axedu.common.security.CurrentUser;
import com.axedu.config.WebConfig;
import com.axedu.dto.chat.ContactResponse;
import com.axedu.entity.enums.UserRole;
import com.axedu.service.ContactService;

import lombok.RequiredArgsConstructor;

/** 새 대화 상대 (계약서 3.2) — 페이지네이션 없음 */
@RestController
@RequestMapping(WebConfig.API_BASE + "/chat/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ApiResponse<List<ContactResponse>> list(@CurrentUser AuthUser user,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(required = false) UserRole role,
                                                   @RequestParam(required = false) Integer grade,
                                                   @RequestParam(required = false) Integer classNo) {
        return ApiResponse.ok(contactService.list(user, q, role, grade, classNo));
    }
}
