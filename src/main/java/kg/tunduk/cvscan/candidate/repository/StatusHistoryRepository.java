package kg.tunduk.cvscan.candidate.repository;

import java.util.List;
import java.util.UUID;
import kg.tunduk.cvscan.candidate.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

    List<StatusHistory> findByCandidateIdOrderByChangedAtDesc(String candidateId);
}
