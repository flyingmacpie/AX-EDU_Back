package com.axedu.dto.complaint;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.axedu.dto.user.UserSummary;
import com.axedu.entity.enums.ComplaintStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Complaint (계약서 2.13).
 * originalText·aiExplanation 은 교사에게는 키 자체를 빼야 하고(필드 = null → 생략),
 * 학부모에게는 값이 없어도 "aiExplanation": null 로 내려야 한다(Optional.empty() → null).
 */
public record ComplaintResponse(
        String id,
        ComplaintStatus status,
        UserSummary parent,
        UserSummary teacher,
        UserSummary student,
        String roomId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Optional<String> originalText,
        @JsonInclude(JsonInclude.Include.NON_NULL) Optional<AiExplanationResponse> aiExplanation,
        SummaryCardResponse summaryCard,
        TeacherReplyResponse teacherReply,
        EscalationResponse escalation,
        OffsetDateTime createdAt,
        OffsetDateTime submittedAt,
        OffsetDateTime closedAt) {
}
