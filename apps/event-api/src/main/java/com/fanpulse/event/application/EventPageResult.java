package com.fanpulse.event.application;

import java.util.List;

public record EventPageResult(
        List<EventSummaryResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
