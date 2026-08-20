package kg.tunduk.cvscan.candidate.service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import org.springframework.stereotype.Component;

/**
 * Generates business ids for candidates created via REST (the contract has no
 * {@code id} field on {@code CandidateWriteRequest}), in the same slug style
 * as the {@code candidateId} values delivered by the {@code cv.parsed} Kafka topic.
 */
@Component
public class SlugIdGenerator {

    private static final Map<Character, String> TRANSLITERATION = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sch"), Map.entry('ъ', ""),
            Map.entry('ы', "y"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
            Map.entry('я', "ya"), Map.entry('ң', "ng"), Map.entry('ө', "o"), Map.entry('ү', "u")
    );

    private static final String SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 5;

    private final CandidateRepository candidateRepository;
    private final SecureRandom random = new SecureRandom();

    public SlugIdGenerator(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    public String generate(String name) {
        String base = slugify(name);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidateId = base + "-" + randomSuffix();
            if (!candidateRepository.existsById(candidateId)) {
                return candidateId;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String slugify(String name) {
        StringBuilder transliterated = new StringBuilder();
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            transliterated.append(TRANSLITERATION.getOrDefault(c, String.valueOf(c)));
        }
        String slug = Arrays.stream(transliterated.toString().split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .limit(2)
                .collect(Collectors.joining("-"));
        return slug.isBlank() ? "candidate" : slug;
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(SUFFIX_ALPHABET.charAt(random.nextInt(SUFFIX_ALPHABET.length())));
        }
        return suffix.toString();
    }
}
