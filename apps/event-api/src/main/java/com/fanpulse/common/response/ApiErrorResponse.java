package com.fanpulse.common.response;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String traceId
) {
}
