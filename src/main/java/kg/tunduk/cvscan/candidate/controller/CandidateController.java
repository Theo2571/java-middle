package kg.tunduk.cvscan.candidate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import kg.tunduk.cvscan.candidate.dto.CandidatePage;
import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.dto.CandidateWriteRequest;
import kg.tunduk.cvscan.candidate.dto.StatusChangeRequest;
import kg.tunduk.cvscan.candidate.dto.StatusHistoryEntry;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.service.CandidateService;
import kg.tunduk.cvscan.candidate.service.StatusService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    private final CandidateService candidateService;
    private final StatusService statusService;

    public CandidateController(CandidateService candidateService, StatusService statusService) {
        this.candidateService = candidateService;
        this.statusService = statusService;
    }

    @GetMapping
    public CandidatePage list(@RequestParam(required = false) Verdict verdict,
                               @RequestParam(required = false) CandidateStatus status,
                               @RequestParam(required = false) String position,
                               @RequestParam(required = false) String search,
                               @RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                               @RequestParam(required = false) String sort) {
        Pageable pageable = PageRequest.of(page, size, SortParser.parse(sort));
        return candidateService.list(verdict, status, position, search, pageable);
    }

    @GetMapping("/{id}")
    public CandidateResponse get(@PathVariable String id) {
        return candidateService.get(id);
    }

    @PostMapping
    public ResponseEntity<CandidateResponse> create(@Valid @RequestBody CandidateWriteRequest request) {
        CandidateResponse response = candidateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public CandidateResponse update(@PathVariable String id, @Valid @RequestBody CandidateWriteRequest request) {
        return candidateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        candidateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public CandidateResponse changeStatus(@PathVariable String id, @Valid @RequestBody StatusChangeRequest request) {
        return statusService.changeStatus(id, request.status(), request.comment());
    }

    @GetMapping("/{id}/status-history")
    public List<StatusHistoryEntry> getStatusHistory(@PathVariable String id) {
        return statusService.getHistory(id);
    }
}
