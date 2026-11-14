package com.axedu.entity;

import java.time.OffsetDateTime;

import com.axedu.entity.enums.ComplaintStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 민원 1건 (complaints). 상태 전이는 version 컬럼으로 낙관적 잠금 */
@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_teacher_id_status_submitted_at", columnList = "teacher_id, status, submitted_at"),
        @Index(name = "idx_complaints_parent_id_created_at", columnList = "parent_id, created_at"),
        @Index(name = "idx_complaints_student_id_created_at", columnList = "student_id, created_at"),
        @Index(name = "idx_complaints_room_id_status", columnList = "room_id, status"),
        @Index(name = "idx_complaints_school_id_status", columnList = "school_id, status")})
@Getter
@Setter
@NoArgsConstructor
public class Complaint {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "school_id", length = 32, nullable = false)
    private String schoolId;

    @Column(name = "room_id", length = 32, nullable = false)
    private String roomId;

    @Column(name = "parent_id", length = 32, nullable = false)
    private String parentId;

    @Column(name = "teacher_id", length = 32, nullable = false)
    private String teacherId;

    @Column(name = "student_id", length = 32, nullable = false)
    private String studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintStatus status = ComplaintStatus.EXPLAINED;

    /** 학부모 원문 AES-256-GCM 암호문. 교사 조회에서 제외 */
    @Column(name = "original_text_enc", nullable = false, length = 16384)
    private byte[] originalTextEnc;

    @Column(name = "submitted_card_version")
    private Integer submittedCardVersion;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "replied_at")
    private OffsetDateTime repliedAt;

    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
