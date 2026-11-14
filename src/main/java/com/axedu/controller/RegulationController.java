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
import com.axedu.dto.regulation.RegulationResponse;
import com.axedu.service.RegulationService;

import lombok.RequiredArgsConstructor;

/** GET /regulations (계약서 4.13). TODO(규정 API): 검색·등록·삭제 차후 작성 — 지금은 빈 목록 */
@RestController
@RequestMapping(WebConfig.API_BASE + "/regulations")
@RequiredArgsConstructor
public class RegulationController {

    private final RegulationService regulationService;

    @GetMapping
    public ApiResponse<List<RegulationResponse>> search(@CurrentUser AuthUser user, @RequestParam(required = false) String q) {
        return ApiResponse.ok(regulationService.search(user, q));
    }
}
