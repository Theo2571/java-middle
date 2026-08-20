package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.dto.CandidatePage;
import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.dto.CandidateWriteRequest;
import kg.tunduk.cvscan.candidate.exception.CandidateNotFoundException;
import kg.tunduk.cvscan.candidate.exception.DuplicateEmailException;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import kg.tunduk.cvscan.candidate.repository.CandidateSpecifications;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateMapper mapper;
    private final SlugIdGenerator slugIdGenerator;

    public CandidateService(CandidateRepository candidateRepository, CandidateMapper mapper,
                             SlugIdGenerator slugIdGenerator) {
        this.candidateRepository = candidateRepository;
        this.mapper = mapper;
        this.slugIdGenerator = slugIdGenerator;
    }

    @Transactional(readOnly = true)
    public CandidatePage list(Verdict verdict, CandidateStatus status, String position, String search, Pageable pageable) {
        List<Specification<Candidate>> filters = Stream.of(
                        CandidateSpecifications.hasVerdict(verdict),
                        CandidateSpecifications.hasStatus(status),
                        CandidateSpecifications.hasPosition(position),
                        CandidateSpecifications.nameContains(search))
                .filter(Objects::nonNull)
                .toList();
        Specification<Candidate> spec = Specification.allOf(filters);

        Page<Candidate> page = candidateRepository.findAll(spec, pageable);
        return new CandidatePage(
                page.getContent().stream().map(mapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public CandidateResponse get(String id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public CandidateResponse create(CandidateWriteRequest request) {
        if (candidateRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        String id = slugIdGenerator.generate(request.name());
        Candidate candidate = mapper.toEntity(id, request);
        try {
            candidate = candidateRepository.save(candidate);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(request.email());
        }
        return mapper.toResponse(candidate);
    }

    @Transactional
    public CandidateResponse update(String id, CandidateWriteRequest request) {
        Candidate candidate = findOrThrow(id);
        if (candidateRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateEmailException(request.email());
        }
        mapper.updateEntity(candidate, request);
        try {
            candidate = candidateRepository.save(candidate);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(request.email());
        }
        return mapper.toResponse(candidate);
    }

    @Transactional
    public void delete(String id) {
        Candidate candidate = findOrThrow(id);
        candidateRepository.delete(candidate);
    }

    private Candidate findOrThrow(String id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
    }
}
