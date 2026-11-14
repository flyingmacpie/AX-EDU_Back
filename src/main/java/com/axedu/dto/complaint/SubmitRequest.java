package com.axedu.dto.complaint;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "확인하고 보내기")
public record SubmitRequest(
        @Schema(description = "학부모가 확인한 카드 버전 (최신이 아니면 409)", example = "1") Integer cardVersion) {
}
