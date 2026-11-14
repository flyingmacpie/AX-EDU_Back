package com.axedu.dto.complaint;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "민원 작성 (학부모)")
public record CreateComplaintRequest(
        @Schema(description = "민원을 연 채팅방 ID", example = "room_p1") String roomId,
        @Schema(description = "자녀 학생 ID (자녀가 한 명이면 생략 가능)", example = "usr_s1") String studentId,
        @Schema(description = "민원 원문 (최대 2000자). 로컬 스텁에서 [AI실패] 를 넣으면 첫 AI 시도 실패", example = "아이가 수행평가 점수를 이해하지 못하고 있어요. 채점 기준을 알고 싶습니다.") String content,
        @Schema(description = "POST /files 로 받은 파일 ID 목록") List<String> attachmentIds) {
}
