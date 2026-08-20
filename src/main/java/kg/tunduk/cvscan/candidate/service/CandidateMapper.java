package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.dto.CandidateWriteRequest;
import kg.tunduk.cvscan.candidate.dto.StatusHistoryEntry;
import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.StatusHistory;
import org.springframework.stereotype.Component;

@Component
public class CandidateMapper {

    public Candidate toEntity(String id, CandidateWriteRequest request) {
        return Candidate.builder()
                .id(id)
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .position(request.position())
                .posLabel(request.posLabel())
                .city(request.city())
                .telegram(request.telegram())
                .totalExp(request.totalExp())
                .stack(request.stack())
                .education(request.education())
                .verdict(request.verdict())
                .status(CandidateStatus.NEW)
                .summary(request.summary())
                .criteria(request.criteria())
                .experience(request.experience())
                .questions(request.questions())
                .build();
    }

    public void updateEntity(Candidate candidate, CandidateWriteRequest request) {
        candidate.setName(request.name());
        candidate.setEmail(request.email());
        candidate.setPhone(request.phone());
        candidate.setPosition(request.position());
        candidate.setPosLabel(request.posLabel());
        candidate.setCity(request.city());
        candidate.setTelegram(request.telegram());
        candidate.setTotalExp(request.totalExp());
        candidate.setStack(request.stack());
        candidate.setEducation(request.education());
        candidate.setVerdict(request.verdict());
        candidate.setSummary(request.summary());
        candidate.setCriteria(request.criteria());
        candidate.setExperience(request.experience());
        candidate.setQuestions(request.questions());
    }

    public Candidate fromEvent(CvParsedEvent event) {
        return Candidate.builder()
                .id(event.candidateId())
                .name(event.name())
                .email(event.email())
                .phone(event.phone())
                .position(event.position())
                .posLabel(event.posLabel())
                .city(event.city())
                .telegram(event.telegram())
                .totalExp(event.totalExp())
                .stack(event.stack())
                .education(event.education())
                .verdict(event.verdict())
                .status(CandidateStatus.NEW)
                .summary(event.summary())
                .criteria(event.criteria())
                .experience(event.experience())
                .questions(event.questions())
                .parsedAt(event.parsedAt())
                .build();
    }

    public CandidateResponse toResponse(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.getPosition(),
                candidate.getPosLabel(),
                candidate.getCity(),
                candidate.getTelegram(),
                candidate.getTotalExp(),
                candidate.getStack(),
                candidate.getEducation(),
                candidate.getVerdict(),
                candidate.getSummary(),
                candidate.getCriteria(),
                candidate.getExperience(),
                candidate.getQuestions(),
                candidate.getStatus(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }

    public StatusHistoryEntry toHistoryEntry(StatusHistory history) {
        return new StatusHistoryEntry(
                history.getId(),
                history.getCandidateId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getComment(),
                history.getChangedAt()
        );
    }
}
