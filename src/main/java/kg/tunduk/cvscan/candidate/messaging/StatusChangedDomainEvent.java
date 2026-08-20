package kg.tunduk.cvscan.candidate.messaging;

import java.time.Instant;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;

/**
 * Internal Spring application event, published once a status transition is
 * persisted. {@link StatusChangedProducer} listens for it after the
 * surrounding transaction commits, so a Kafka message is never sent for a
 * DB write that ends up rolled back.
 */
public record StatusChangedDomainEvent(
        String candidateId,
        CandidateStatus fromStatus,
        CandidateStatus toStatus,
        Instant changedAt
) {
}
