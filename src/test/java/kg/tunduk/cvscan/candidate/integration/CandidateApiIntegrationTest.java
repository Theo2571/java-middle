package kg.tunduk.cvscan.candidate.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class CandidateApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandidateRepository candidateRepository;

    @AfterEach
    void cleanUp() {
        candidateRepository.deleteAll(candidateRepository.findAllById(createdIds));
        createdIds.clear();
    }

    private final java.util.List<String> createdIds = new java.util.ArrayList<>();

    @Test
    void listsCandidatesFilteredByVerdictStatusAndSearch() throws Exception {
        // "qa-isolation-test" is a position no seed/other-test candidate uses,
        // so combining it into every query isolates these assertions from the
        // 12 seeded candidates (and any leftovers from other test methods)
        // sharing the same Testcontainers database.
        String position = "qa-isolation-test";
        seedCandidate("filter-fit-new", "filter.fit.new@email.com", Verdict.FIT, CandidateStatus.NEW, "Filterova Zulfiya", position);
        seedCandidate("filter-fit-invited", "filter.fit.invited@email.com", Verdict.FIT, CandidateStatus.INVITED, "Filterov Bekzhan", position);
        seedCandidate("filter-partial-new", "filter.partial.new@email.com", Verdict.PARTIAL, CandidateStatus.NEW, "Otherova Zulfiya", position);

        mockMvc.perform(get("/api/v1/candidates")
                        .param("verdict", "FIT")
                        .param("status", "NEW")
                        .param("position", position)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("filter-fit-new"));

        mockMvc.perform(get("/api/v1/candidates")
                        .param("search", "Zulfiya")
                        .param("position", position)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder("filter-fit-new", "filter-partial-new")));
    }

    @Test
    void createWithDuplicateEmailReturns409() throws Exception {
        seedCandidate("dup-email-existing", "dup.email@email.com", Verdict.FIT, CandidateStatus.NEW, "Существующий Кандидат");

        String body = """
                {"name":"Новый Кандидат","email":"dup.email@email.com","position":"java-middle","verdict":"FIT"}
                """;

        mockMvc.perform(post("/api/v1/candidates").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));
    }

    @Test
    void validStatusTransitionIsRecordedInHistory() throws Exception {
        seedCandidate("transition-ok", "transition.ok@email.com", Verdict.FIT, CandidateStatus.NEW, "Переходов Валид Валидович");

        mockMvc.perform(patch("/api/v1/candidates/transition-ok/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_REVIEW","comment":"looks good"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        mockMvc.perform(get("/api/v1/candidates/transition-ok/status-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromStatus").value("NEW"))
                .andExpect(jsonPath("$[0].toStatus").value("IN_REVIEW"))
                .andExpect(jsonPath("$[0].comment").value("looks good"));
    }

    @Test
    void invalidStatusTransitionReturns422() throws Exception {
        seedCandidate("transition-bad", "transition.bad@email.com", Verdict.FIT, CandidateStatus.NEW, "Переходов Инвалид Инвалидович");

        mockMvc.perform(patch("/api/v1/candidates/transition-bad/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APPROVED"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void deletingUnknownCandidateReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/candidates/does-not-exist-" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CANDIDATE_NOT_FOUND"));
    }

    private void seedCandidate(String id, String email, Verdict verdict, CandidateStatus status, String name) {
        seedCandidate(id, email, verdict, status, name, "java-middle");
    }

    private void seedCandidate(String id, String email, Verdict verdict, CandidateStatus status, String name, String position) {
        candidateRepository.save(Candidate.builder()
                .id(id)
                .name(name)
                .email(email)
                .position(position)
                .verdict(verdict)
                .status(status)
                .criteria(java.util.List.of())
                .experience(java.util.List.of())
                .questions(java.util.List.of())
                .build());
        createdIds.add(id);
    }
}
