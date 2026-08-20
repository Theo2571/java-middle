package kg.tunduk.cvscan.candidate.dto;

import java.util.List;

public record CandidatePage(
        List<CandidateResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
