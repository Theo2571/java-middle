package kg.tunduk.cvscan.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.exception.CandidateNotFoundException;
import kg.tunduk.cvscan.candidate.exception.InvalidStatusTransitionException;
import kg.tunduk.cvscan.candidate.messaging.StatusChangedDomainEvent;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import kg.tunduk.cvscan.candidate.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new StatusService(candidateRepository, statusHistoryRepository, new CandidateMapper(), eventPublisher);
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, IN_REVIEW",
            "IN_REVIEW, INVITED",
            "IN_REVIEW, REJECTED",
            "INVITED, APPROVED",
            "INVITED, REJECTED"
    })
    void allowsEveryValidTransition(CandidateStatus from, CandidateStatus to) {
        Candidate candidate = candidateWithStatus(from);
        when(candidateRepository.findById("ivanov")).thenReturn(Optional.of(candidate));

        CandidateResponse response = statusService.changeStatus("ivanov", to, "комментарий");

        assertThat(response.status()).isEqualTo(to);
        verify(statusHistoryRepository).save(argThat(history ->
                history.getCandidateId().equals("ivanov")
                        && history.getFromStatus() == from
                        && history.getToStatus() == to
                        && history.getComment().equals("комментарий")));
        verify(eventPublisher).publishEvent(argThat((StatusChangedDomainEvent event) ->
                event.candidateId().equals("ivanov") && event.fromStatus() == from && event.toStatus() == to));
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, APPROVED",
            "NEW, REJECTED",
            "NEW, NEW",
            "IN_REVIEW, NEW",
            "IN_REVIEW, APPROVED",
            "INVITED, IN_REVIEW",
            "INVITED, NEW",
            "APPROVED, IN_REVIEW",
            "APPROVED, REJECTED",
            "REJECTED, NEW",
            "REJECTED, INVITED"
    })
    void rejectsEveryInvalidTransition(CandidateStatus from, CandidateStatus to) {
        Candidate candidate = candidateWithStatus(from);
        when(candidateRepository.findById("ivanov")).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> statusService.changeStatus("ivanov", to, null))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(statusHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsWhenCandidateDoesNotExist() {
        when(candidateRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusService.changeStatus("unknown", CandidateStatus.IN_REVIEW, null))
                .isInstanceOf(CandidateNotFoundException.class);

        verify(statusHistoryRepository, never()).save(any());
    }

    private Candidate candidateWithStatus(CandidateStatus status) {
        return Candidate.builder()
                .id("ivanov")
                .name("Иванов Иван Иванович")
                .email("ivanov@email.com")
                .position("java-middle")
                .verdict(Verdict.FIT)
                .status(status)
                .criteria(List.of())
                .experience(List.of())
                .questions(List.of())
                .build();
    }
}
