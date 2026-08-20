package kg.tunduk.cvscan.candidate.model;

import jakarta.validation.constraints.NotBlank;

public record ExperienceItem(
        @NotBlank String period,
        @NotBlank String company,
        @NotBlank String title,
        @NotBlank String duration
) {
}
