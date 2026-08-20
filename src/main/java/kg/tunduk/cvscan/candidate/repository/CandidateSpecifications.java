package kg.tunduk.cvscan.candidate.repository;

import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CandidateSpecifications {

    private CandidateSpecifications() {
    }

    public static Specification<Candidate> hasVerdict(Verdict verdict) {
        if (verdict == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("verdict"), verdict);
    }

    public static Specification<Candidate> hasStatus(CandidateStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Candidate> hasPosition(String position) {
        if (!StringUtils.hasText(position)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("position"), position);
    }

    public static Specification<Candidate> nameContains(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }
}
