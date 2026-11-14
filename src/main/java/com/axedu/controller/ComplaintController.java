package com.axedu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.axedu.dto.complaint.ComplaintResponse;
import com.axedu.dto.complaint.CreateComplaintRequest;
import com.axedu.dto.complaint.EditCardRequest;
import com.axedu.dto.complaint.EscalationRequest;
import com.axedu.dto.complaint.EscalationResponse;
import com.axedu.dto.complaint.ReplyDraftRequest;
import com.axedu.dto.complaint.ReplyDraftResponse;
import com.axedu.dto.complaint.ReplyRequest;
import com.axedu.dto.complaint.ReplyResultResponse;
import com.axedu.dto.complaint.RevisionRequest;
import com.axedu.dto.complaint.SubmitRequest;
import com.axedu.dto.complaint.SummaryCardResponse;
import com.axedu.service.ComplaintService;
import com.axedu.service.SummaryCardService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 민원 (계약서 4장). 상태 변경 API 는 모두 전체 Complaint, 카드 API 는 카드 자체, 도움 요청은 전체 Escalation 을 data 로.
 */
@RestController
@RequestMapping(WebConfig.API_BASE + "/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private static final String IDEMPOTENCY = "Idempotency-Key";

    private final ComplaintService complaintService;
    private final SummaryCardService summaryCardService;

    // ---------------- 학부모 ----------------

    @PostMapping
    public ResponseEntity<ApiResponse<ComplaintResponse>> create(@CurrentUser AuthUser user,
                                                                 @RequestHeader(name = IDEMPOTENCY, required = false) String key,
                                                                 @RequestBody CreateComplaintRequest req) {
        return created(complaintService.create(user, req, key));
    }

    @PostMapping("/{id}/explanation/retry")
    public ApiResponse<ComplaintResponse> retry(@CurrentUser AuthUser user, @PathVariable String id) {
        return ApiResponse.ok(complaintService.retryExplanation(user, id));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ComplaintResponse> close(@CurrentUser AuthUser user, @PathVariable String id) {
        return ApiResponse.ok(complaintService.close(user, id));
    }

    @PostMapping("/{id}/summary-card")
    public ResponseEntity<ApiResponse<SummaryCardResponse>> createCard(@CurrentUser AuthUser user, @PathVariable String id) {
        return created(summaryCardService.create(user, id));
    }

    @PostMapping("/{id}/summary-card/revisions")
    public ApiResponse<SummaryCardResponse> revise(@CurrentUser AuthUser user, @PathVariable String id,
                                                   @RequestBody RevisionRequest req) {
        return ApiResponse.ok(summaryCardService.revise(user, id, req));
    }

    @PatchMapping("/{id}/summary-card")
    public ApiResponse<SummaryCardResponse> edit(@CurrentUser AuthUser user, @PathVariable String id,
                                                 @RequestBody EditCardRequest req) {
        return ApiResponse.ok(summaryCardService.edit(user, id, req));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ComplaintResponse> submit(@CurrentUser AuthUser user, @PathVariable String id,
                                                 @RequestHeader(name = IDEMPOTENCY, required = false) String key,
                                                 @RequestBody SubmitRequest req) {
        return ApiResponse.ok(complaintService.submit(user, id, req, key));
    }

    // ---------------- 조회 ----------------

    @GetMapping
    public ApiResponse<List<ComplaintResponse>> list(@CurrentUser AuthUser user,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String studentId,
                                                     @RequestParam(required = false) String cursor,
                                                     @RequestParam(required = false) Integer limit) {
        ComplaintService.ComplaintPage page = complaintService.list(user, status, studentId, cursor, limit);
        return ApiResponse.ok(page.items(), PageMeta.cursor(page.nextCursor()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ComplaintResponse> get(@CurrentUser AuthUser user, @PathVariable String id, HttpServletRequest request) {
        return ApiResponse.ok(complaintService.get(user, id, request.getRemoteAddr()));
    }

    // ---------------- 교사 ----------------

    @PostMapping("/{id}/acknowledge")
    public ApiResponse<ComplaintResponse> acknowledge(@CurrentUser AuthUser user, @PathVariable String id) {
        return ApiResponse.ok(complaintService.acknowledge(user, id));
    }

    @PostMapping("/{id}/reply-drafts")
    public ApiResponse<ReplyDraftResponse> replyDraft(@CurrentUser AuthUser user, @PathVariable String id,
                                                      @RequestBody ReplyDraftRequest req) {
        return ApiResponse.ok(complaintService.createReplyDraft(user, id, req));
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<ReplyResultResponse> reply(@CurrentUser AuthUser user, @PathVariable String id, @RequestBody ReplyRequest req) {
        return ApiResponse.ok(complaintService.reply(user, id, req));
    }

    @PostMapping("/{id}/escalations")
    public ResponseEntity<ApiResponse<EscalationResponse>> escalate(@CurrentUser AuthUser user, @PathVariable String id,
                                                                    @RequestBody EscalationRequest req) {
        return created(complaintService.escalate(user, id, req));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<ComplaintResponse> resolve(@CurrentUser AuthUser user, @PathVariable String id) {
        return ApiResponse.ok(complaintService.resolve(user, id));
    }

    private static <T> ResponseEntity<ApiResponse<T>> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }
}
