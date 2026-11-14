package com.axedu.dto.chat;

import java.util.List;

/** 프론트는 첨부가 있어도 type 을 항상 TEXT 로 보낸다 → 서버가 IMAGE/FILE 결정 */
public record SendMessageRequest(String clientMessageId, String type, String content, List<String> attachmentIds) {
}
