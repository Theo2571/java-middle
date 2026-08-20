package kg.tunduk.cvscan.candidate.exception;

import kg.tunduk.cvscan.candidate.model.CandidateStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(CandidateStatus from, CandidateStatus to) {
        super("Переход %s → %s недопустим".formatted(from, to));
    }
}
