package kg.tunduk.cvscan.candidate.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.messaging.CvParsedConsumer;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.CriteriaItem;
import kg.tunduk.cvscan.candidate.model.CriteriaResult;
import kg.tunduk.cvscan.candidate.model.ExperienceItem;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mirrors the exact duplicate scenario from {@code test-events/cv-parsed-bulk.ndjson}:
 * the same candidateId + parsedAt pair delivered twice must create the
 * candidate only once.
 */
@ExtendWith(MockitoExtension.class)
class CvParsedConsumerTest {

    @Mock
    private CandidateRepository candidateRepository;

    private CvParsedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CvParsedConsumer(candidateRepository, new CandidateMapper());
    }

    @Test
    void createsCandidateForNewEvent() {
        CvParsedEvent event = sampleEvent();
        when(candidateRepository.existsByIdAndParsedAt(event.candidateId(), event.parsedAt())).thenReturn(false);

        consumer.onMessage(event);

        verify(candidateRepository).save(argThat(candidate ->
                candidate.getId().equals(event.candidateId())
                        && candidate.getStatus() == CandidateStatus.NEW
                        && candidate.getParsedAt().equals(event.parsedAt())));
    }

    @Test
    void ignoresDuplicateEventWithSameCandidateIdAndParsedAt() {
        CvParsedEvent event = sampleEvent();
        when(candidateRepository.existsByIdAndParsedAt(event.candidateId(), event.parsedAt())).thenReturn(true);

        consumer.onMessage(event);

        verify(candidateRepository, never()).save(any());
    }

    private CvParsedEvent sampleEvent() {
        return new CvParsedEvent(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "asanov-bakyt",
                Instant.parse("2026-05-10T09:00:00Z"),
                "Асанов Бакыт Эркинович",
                "java-middle",
                "Java — ведущий программист",
                "asanov.bakyt@email.com",
                "+996 700 111222",
                "Бишкек",
                "@asanov_dev",
                "~4 г.",
                "Java 17, Spring Boot 3, PostgreSQL, Kafka, Testcontainers, Gradle, Docker",
                "КГТУ им. Раззакова, ИТ, 2021",
                Verdict.FIT,
                "Backend-разработчик с 4 годами коммерческого опыта на Java.",
                List.of(new CriteriaItem("java_spring", CriteriaResult.OK, "4 года коммерческой разработки")),
                List.of(new ExperienceItem("2023-03 — н.в.", "ПКБ «Финтех KG»", "Java Developer", "~2 г.")),
                List.of("Kafka: как настраивали consumer group при нескольких репликах сервиса?")
        );
    }
}
