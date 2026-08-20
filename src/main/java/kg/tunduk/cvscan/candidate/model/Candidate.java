package kg.tunduk.cvscan.candidate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    private String id;

    private String name;

    private String email;

    private String phone;

    private String position;

    private String posLabel;

    private String city;

    private String telegram;

    private String totalExp;

    @Column(columnDefinition = "text")
    private String stack;

    @Column(columnDefinition = "text")
    private String education;

    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    @Enumerated(EnumType.STRING)
    private CandidateStatus status;

    @Column(columnDefinition = "text")
    private String summary;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    private List<CriteriaItem> criteria = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ExperienceItem> experience = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> questions = new ArrayList<>();

    private Instant parsedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
