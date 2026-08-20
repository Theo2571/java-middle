package kg.tunduk.cvscan.candidate.exception;

public class CandidateNotFoundException extends RuntimeException {

    public CandidateNotFoundException(String candidateId) {
        super("Кандидат '%s' не найден".formatted(candidateId));
    }
}
