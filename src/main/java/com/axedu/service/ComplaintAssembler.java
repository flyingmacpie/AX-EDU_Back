package com.axedu.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.axedu.common.security.AuthUser;
import com.axedu.common.security.TextCipher;
import com.axedu.common.util.TimeUtil;
import com.axedu.dto.chat.TimeRange;
import com.axedu.dto.complaint.AiExplanationResponse;
import com.axedu.dto.complaint.ComplaintResponse;
import com.axedu.dto.complaint.EscalationResponse;
import com.axedu.dto.complaint.SummaryCardResponse;
import com.axedu.dto.complaint.TeacherReplyResponse;
import com.axedu.dto.user.UserSummary;
import com.axedu.entity.Complaint;
import com.axedu.entity.ComplaintAiExplanation;
import com.axedu.entity.ComplaintExplanationCitation;
import com.axedu.entity.Escalation;
import com.axedu.entity.SummaryCard;
import com.axedu.entity.SummaryCardRegulation;
import com.axedu.entity.enums.AiStatus;
import com.axedu.entity.enums.UserRole;
import com.axedu.repository.ComplaintAiExplanationRepository;
import com.axedu.repository.ComplaintExplanationCitationRepository;
import com.axedu.repository.ComplaintReplyRepository;
import com.axedu.repository.EscalationRepository;
import com.axedu.repository.SummaryCardRegulationRepository;
import com.axedu.repository.SummaryCardRepository;

import lombok.RequiredArgsConstructor;

/**
 * 민원 응답 조립 (계약서 2.13 역할별 응답 모양).
 * 교사: originalText·aiExplanation 키 생략, summaryCard 는 학부모가 제출한 버전.
 * 학부모·대응팀·관리자: 원문 포함, 최신 카드.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplaintAssembler {

    private final UserService userService;
    private final TextCipher cipher;
    private final SummaryCardRepository cardRepository;
    private final SummaryCardRegulationRepository cardRegulationRepository;
    private final ComplaintAiExplanationRepository explanationRepository;
    private final ComplaintExplanationCitationRepository citationRepository;
    private final ComplaintReplyRepository replyRepository;
    private final EscalationRepository escalationRepository;
    private final RegulationService regulationService;

    public ComplaintResponse view(Complaint c, AuthUser viewer) {
        boolean teacher = viewer.role() == UserRole.TEACHER;
        // 방 밖에서 시작해 아직 제출 전인 민원은 teacher·roomId 가 null
        Map<String, UserSummary> users = userService.summaries(Arrays.asList(c.getParentId(), c.getTeacherId(), c.getStudentId()));

        SummaryCard card;
        if (teacher) {
            card = c.getSubmittedCardVersion() == null ? null
                    : cardRepository.findByComplaintIdAndVersion(c.getId(), c.getSubmittedCardVersion()).orElse(null);
        } else {
            card = cardRepository.findFirstByComplaintIdOrderByVersionDesc(c.getId()).orElse(null);
        }

        Optional<String> originalText = teacher ? null : Optional.of(cipher.decrypt(c.getOriginalTextEnc()));
        Optional<AiExplanationResponse> aiExplanation = teacher ? null : Optional.ofNullable(explanation(c.getId()));

        return new ComplaintResponse(
                c.getId(),
                c.getStatus(),
                users.get(c.getParentId()),
                c.getTeacherId() == null ? null : users.get(c.getTeacherId()),
                users.get(c.getStudentId()),
                c.getRoomId(),
                originalText,
                aiExplanation,
                card == null ? null : card(card, false),
                replyRepository.findById(c.getId())
                        .map(r -> new TeacherReplyResponse(r.getContent(), r.getContactMethod(), r.getScheduledAt(), r.getSentAt()))
                        .orElse(null),
                escalationRepository.findByComplaintId(c.getId()).map(this::escalation).orElse(null),
                c.getCreatedAt(),
                c.getSubmittedAt(),
                c.getClosedAt());
    }

    /** 최신 성공 AI 설명. 없으면 null */
    public AiExplanationResponse explanation(String complaintId) {
        return explanationRepository.findFirstByComplaintIdAndStatusOrderByAttemptDesc(complaintId, AiStatus.SUCCEEDED)
                .map(this::explanation)
                .orElse(null);
    }

    public AiExplanationResponse explanation(ComplaintAiExplanation e) {
        List<String> chunkIds = citationRepository.findByExplanationIdOrderBySortOrderAsc(e.getId()).stream()
                .map(ComplaintExplanationCitation::getRegulationChunkId).toList();
        return new AiExplanationResponse(e.getEmpathy(), e.getReason(), e.getAlternative(),
                regulationService.toResponses(chunkIds), e.getRecommendation());
    }

    /** SummaryCard / SummaryCardResult. withSuggestions=true 는 카드 API 응답에서만 */
    public SummaryCardResponse card(SummaryCard card, boolean withSuggestions) {
        List<String> chunkIds = cardRegulationRepository.findByCardIdOrderBySortOrderAsc(card.getId()).stream()
                .map(SummaryCardRegulation::getRegulationChunkId).toList();
        TimeRange window = card.getPreferredFrom() == null || card.getPreferredTo() == null ? null
                : new TimeRange(TimeUtil.hhmm(card.getPreferredFrom()), TimeUtil.hhmm(card.getPreferredTo()));
        return new SummaryCardResponse(
                card.getVersion(),
                new SummaryCardResponse.BasicInfo(card.getGrade(), card.getClassNo(), card.getReceivedAt(), List.copyOf(card.getTags())),
                card.getAttribution(),
                List.copyOf(card.getSituation()),
                List.copyOf(card.getConcerns()),
                List.copyOf(card.getRequests()),
                new SummaryCardResponse.PreferredContact(List.copyOf(card.getPreferredMethods()), window),
                regulationService.toResponses(chunkIds),
                card.getConfirmedAt(),
                withSuggestions && card.getSuggestions() != null ? List.copyOf(card.getSuggestions()) : null);
    }

    public EscalationResponse escalation(Escalation e) {
        UserSummary assignee = e.getAssigneeId() == null ? null : userService.summary(userService.get(e.getAssigneeId()));
        return new EscalationResponse(e.getId(), List.copyOf(e.getReasonTypes()), e.getDescription(), e.getStage(), e.getStatus(),
                assignee, e.getRequestedAt());
    }
}
