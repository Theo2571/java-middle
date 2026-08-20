package kg.tunduk.cvscan.candidate.dto;

import java.time.Instant;
import java.util.List;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.CriteriaItem;
import kg.tunduk.cvscan.candidate.model.ExperienceItem;
import kg.tunduk.cvscan.candidate.model.Verdict;

public record CandidateResponse(
        String id,
        String name,
        String email,
        String phone,
        String position,
        String posLabel,
        String city,
        String telegram,
        String totalExp,
        String stack,
        String education,
        Verdict verdict,
        String summary,
        List<CriteriaItem> criteria,
        List<ExperienceItem> experience,
        List<String> questions,
        CandidateStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
