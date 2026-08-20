package kg.tunduk.cvscan.candidate.dto;

import java.time.Instant;
import java.util.UUID;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;

public record StatusHistoryEntry(
        UUID id,
        String candidateId,
        CandidateStatus fromStatus,
        CandidateStatus toStatus,
        String comment,
        Instant changedAt
) {
}
