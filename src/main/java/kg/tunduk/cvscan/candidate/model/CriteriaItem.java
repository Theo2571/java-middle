package kg.tunduk.cvscan.candidate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriteriaItem(
        @NotBlank String key,
        @NotNull CriteriaResult result,
        @NotBlank String comment
) {
}
