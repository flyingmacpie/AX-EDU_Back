package com.axedu.dto.chat;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * PATCH /chat/rooms/{id}/settings. 보낸 키만 바꾼다.
 * availableHoursOverride 는 "키 없음"(유지)과 "null"(교사 기본 설정으로 되돌림)을 구분해야 해서 setter 로 받는다.
 */
public class UpdateRoomSettingsRequest {

    private Boolean muted;
    private TimeRange availableHoursOverride;
    private boolean availableHoursOverridePresent;

    public Boolean getMuted() {
        return muted;
    }

    public void setMuted(Boolean muted) {
        this.muted = muted;
    }

    public TimeRange getAvailableHoursOverride() {
        return availableHoursOverride;
    }

    @JsonSetter("availableHoursOverride")
    public void setAvailableHoursOverride(TimeRange availableHoursOverride) {
        this.availableHoursOverride = availableHoursOverride;
        this.availableHoursOverridePresent = true;
    }

    public boolean isAvailableHoursOverridePresent() {
        return availableHoursOverridePresent;
    }
}
