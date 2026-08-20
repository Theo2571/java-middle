package kg.tunduk.cvscan.candidate.dto.event;

import java.time.Instant;
import java.util.UUID;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;

public record StatusChangedEvent(
        UUID eventId,
        String candidateId,
        CandidateStatus fromStatus,
        CandidateStatus toStatus,
        Instant changedAt
) {
}
