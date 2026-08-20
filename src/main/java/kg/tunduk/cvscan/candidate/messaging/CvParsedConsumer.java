package kg.tunduk.cvscan.candidate.messaging;

import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import kg.tunduk.cvscan.candidate.service.CandidateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes {@code cv.parsed} events and idempotently creates candidates.
 * Idempotency key is (candidateId, parsedAt): a redelivery of the same pair
 * must not create a duplicate row. The existence check handles the common
 * case; the {@link DataIntegrityViolationException} catch is a safety net
 * for concurrent redelivery racing past that check, since {@code id} is the
 * table's primary key.
 */
@Component
public class CvParsedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CvParsedConsumer.class);

    private final CandidateRepository candidateRepository;
    private final CandidateMapper mapper;

    public CvParsedConsumer(CandidateRepository candidateRepository, CandidateMapper mapper) {
        this.candidateRepository = candidateRepository;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.cv-parsed}")
    @Transactional
    public void onMessage(CvParsedEvent event) {
        if (candidateRepository.existsByIdAndParsedAt(event.candidateId(), event.parsedAt())) {
            log.info("Duplicate cv.parsed event ignored: eventId={}, candidateId={}, parsedAt={}",
                    event.eventId(), event.candidateId(), event.parsedAt());
            return;
        }

        Candidate candidate = mapper.fromEvent(event);
        try {
            // saveAndFlush (not save): candidateId is an assigned (non-generated)
            // key, so a plain save() only schedules the INSERT for the next flush
            // instead of executing it now - a concurrent redelivery's constraint
            // violation would otherwise surface at transaction commit, past this
            // try/catch, crashing the listener instead of being logged as a no-op.
            candidateRepository.saveAndFlush(candidate);
            log.info("Candidate created from cv.parsed event: eventId={}, candidateId={}",
                    event.eventId(), event.candidateId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate cv.parsed event ignored after constraint violation: eventId={}, candidateId={}",
                    event.eventId(), event.candidateId());
        }
    }
}
