package com.axedu.dto.complaint;

import java.util.List;

public record CreateComplaintRequest(String roomId, String studentId, String content, List<String> attachmentIds) {
}
