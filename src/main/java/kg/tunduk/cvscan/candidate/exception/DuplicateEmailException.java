package kg.tunduk.cvscan.candidate.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Кандидат с email '%s' уже существует".formatted(email));
    }
}
