package com.fanpulse.event.presentation;

import com.fanpulse.event.application.EventPageResult;

import java.util.List;

public record EventPageResponse(
        List<EventSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    static EventPageResponse from(EventPageResult result) {
        return new EventPageResponse(
                result.content().stream().map(EventSummaryResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
