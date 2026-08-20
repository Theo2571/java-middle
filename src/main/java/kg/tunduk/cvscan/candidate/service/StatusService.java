package kg.tunduk.cvscan.candidate.service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.dto.StatusHistoryEntry;
import kg.tunduk.cvscan.candidate.exception.CandidateNotFoundException;
import kg.tunduk.cvscan.candidate.exception.InvalidStatusTransitionException;
import kg.tunduk.cvscan.candidate.messaging.StatusChangedDomainEvent;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.StatusHistory;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import kg.tunduk.cvscan.candidate.repository.StatusHistoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single choke point for candidate status transitions: validates the
 * transition, records history and updates the candidate. This is the only
 * place that is allowed to change {@link Candidate#getStatus()}.
 */
@Service
public class StatusService {

    private static final Map<CandidateStatus, Set<CandidateStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(CandidateStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(CandidateStatus.NEW, EnumSet.of(CandidateStatus.IN_REVIEW));
        ALLOWED_TRANSITIONS.put(CandidateStatus.IN_REVIEW, EnumSet.of(CandidateStatus.INVITED, CandidateStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(CandidateStatus.INVITED, EnumSet.of(CandidateStatus.APPROVED, CandidateStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(CandidateStatus.APPROVED, EnumSet.noneOf(CandidateStatus.class));
        ALLOWED_TRANSITIONS.put(CandidateStatus.REJECTED, EnumSet.noneOf(CandidateStatus.class));
    }

    private final CandidateRepository candidateRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CandidateMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public StatusService(CandidateRepository candidateRepository, StatusHistoryRepository statusHistoryRepository,
                          CandidateMapper mapper, ApplicationEventPublisher eventPublisher) {
        this.candidateRepository = candidateRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CandidateResponse changeStatus(String candidateId, CandidateStatus newStatus, String comment) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));

        CandidateStatus currentStatus = candidate.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        Instant changedAt = Instant.now();
        candidate.setStatus(newStatus);
        // saveAndFlush (not save): @PreUpdate only fires at flush time, so a
        // plain save() would leave candidate.updatedAt stale for the response
        // built below, before the transaction commits.
        candidateRepository.saveAndFlush(candidate);

        statusHistoryRepository.save(StatusHistory.builder()
                .id(UUID.randomUUID())
                .candidateId(candidateId)
                .fromStatus(currentStatus)
                .toStatus(newStatus)
                .comment(comment)
                .changedAt(changedAt)
                .build());

        eventPublisher.publishEvent(new StatusChangedDomainEvent(candidateId, currentStatus, newStatus, changedAt));

        return mapper.toResponse(candidate);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryEntry> getHistory(String candidateId) {
        if (!candidateRepository.existsById(candidateId)) {
            throw new CandidateNotFoundException(candidateId);
        }
        return statusHistoryRepository.findByCandidateIdOrderByChangedAtDesc(candidateId).stream()
                .map(mapper::toHistoryEntry)
                .toList();
    }
}
