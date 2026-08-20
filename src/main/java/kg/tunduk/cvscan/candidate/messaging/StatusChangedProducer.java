package kg.tunduk.cvscan.candidate.messaging;

import java.util.UUID;
import kg.tunduk.cvscan.candidate.dto.event.StatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes {@code candidate.status.changed} events. Listens for the
 * internal {@link StatusChangedDomainEvent} only after the surrounding
 * transaction commits (AFTER_COMMIT), so a Kafka message is never sent for a
 * status/history write that ultimately rolls back.
 */
@Component
public class StatusChangedProducer {

    private static final Logger log = LoggerFactory.getLogger(StatusChangedProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public StatusChangedProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                  @Value("${app.kafka.topics.status-changed}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(StatusChangedDomainEvent event) {
        StatusChangedEvent payload = new StatusChangedEvent(
                UUID.randomUUID(),
                event.candidateId(),
                event.fromStatus(),
                event.toStatus(),
                event.changedAt()
        );
        kafkaTemplate.send(topic, event.candidateId(), payload);
        log.info("Published candidate.status.changed: candidateId={}, {} -> {}",
                event.candidateId(), event.fromStatus(), event.toStatus());
    }
}
