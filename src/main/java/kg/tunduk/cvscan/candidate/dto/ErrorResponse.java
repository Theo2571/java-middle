package kg.tunduk.cvscan.candidate.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        List<ErrorDetail> details,
        Instant timestamp,
        String path
) {
}
