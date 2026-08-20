package kg.tunduk.cvscan.candidate.dto.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kg.tunduk.cvscan.candidate.model.CriteriaItem;
import kg.tunduk.cvscan.candidate.model.ExperienceItem;
import kg.tunduk.cvscan.candidate.model.Verdict;

public record CvParsedEvent(
        UUID eventId,
        String candidateId,
        Instant parsedAt,
        String name,
        String position,
        String posLabel,
        String email,
        String phone,
        String city,
        String telegram,
        String totalExp,
        String stack,
        String education,
        Verdict verdict,
        String summary,
        List<CriteriaItem> criteria,
        List<ExperienceItem> experience,
        List<String> questions
) {
}
