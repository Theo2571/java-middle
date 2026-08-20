package kg.tunduk.cvscan.candidate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import kg.tunduk.cvscan.candidate.model.CriteriaItem;
import kg.tunduk.cvscan.candidate.model.ExperienceItem;
import kg.tunduk.cvscan.candidate.model.Verdict;

public record CandidateWriteRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Email String email,
        @Pattern(regexp = "^\\+\\d[\\d ]{6,20}$") String phone,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String position,
        String posLabel,
        String city,
        String telegram,
        String totalExp,
        String stack,
        String education,
        @NotNull Verdict verdict,
        String summary,
        @Valid List<CriteriaItem> criteria,
        @Valid List<ExperienceItem> experience,
        List<String> questions
) {
    public CandidateWriteRequest {
        criteria = criteria == null ? List.of() : criteria;
        experience = experience == null ? List.of() : experience;
        questions = questions == null ? List.of() : questions;
    }
}
