package kg.tunduk.cvscan.candidate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidate_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistory {

    @Id
    private UUID id;

    private String candidateId;

    @Enumerated(EnumType.STRING)
    private CandidateStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private CandidateStatus toStatus;

    @Column(columnDefinition = "text")
    private String comment;

    private Instant changedAt;
}
