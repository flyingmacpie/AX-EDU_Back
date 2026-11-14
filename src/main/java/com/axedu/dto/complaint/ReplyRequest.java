package com.axedu.dto.complaint;

import java.time.OffsetDateTime;

import com.axedu.entity.enums.ContactMethod;

public record ReplyRequest(String content, ContactMethod contactMethod, OffsetDateTime scheduledAt) {
}
