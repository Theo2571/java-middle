package kg.tunduk.cvscan.candidate.repository;

import java.time.Instant;
import kg.tunduk.cvscan.candidate.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CandidateRepository extends JpaRepository<Candidate, String>, JpaSpecificationExecutor<Candidate> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, String id);

    boolean existsByIdAndParsedAt(String id, Instant parsedAt);
}
