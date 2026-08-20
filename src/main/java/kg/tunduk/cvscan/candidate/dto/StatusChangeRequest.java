package kg.tunduk.cvscan.candidate.dto;

import jakarta.validation.constraints.NotNull;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;

public record StatusChangeRequest(
        @NotNull CandidateStatus status,
        String comment
) {
}
