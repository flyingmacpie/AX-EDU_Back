package com.axedu.dto.complaint;

import java.util.List;

public record EscalationRequest(List<String> reasonTypes, String description) {
}
