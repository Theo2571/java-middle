package kg.tunduk.cvscan.candidate.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Publishes raw JSON directly onto {@code cv.parsed} (bypassing the
 * application's own producer) to verify the consumer end-to-end against a
 * real broker, mirroring how {@code test-events/cv-parsed-sample.json}
 * would be delivered by kcat in manual testing.
 */
class KafkaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CandidateRepository candidateRepository;

    @Test
    void consumerCreatesCandidateFromCvParsedEvent() {
        KafkaTemplate<String, String> producer = rawJsonProducer();
        String candidateId = "kafka-it-" + UUID.randomUUID().toString().substring(0, 8);
        String payload = sampleEventJson(candidateId, "2026-07-01T09:00:00Z", UUID.randomUUID().toString());

        producer.send("cv.parsed", candidateId, payload);

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(candidateRepository.existsById(candidateId)).isTrue());

        var candidate = candidateRepository.findById(candidateId).orElseThrow();
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.NEW);
        assertThat(candidate.getEmail()).isEqualTo(candidateId + "@email.com");
    }

    @Test
    void duplicateEventWithSameCandidateIdAndParsedAtIsIgnored() {
        KafkaTemplate<String, String> producer = rawJsonProducer();
        String candidateId = "kafka-it-dup-" + UUID.randomUUID().toString().substring(0, 8);
        String parsedAt = "2026-07-02T09:00:00Z";

        producer.send("cv.parsed", candidateId, sampleEventJson(candidateId, parsedAt, UUID.randomUUID().toString()));
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(candidateRepository.existsById(candidateId)).isTrue());

        long countAfterFirstDelivery = candidateRepository.count();

        producer.send("cv.parsed", candidateId, sampleEventJson(candidateId, parsedAt, UUID.randomUUID().toString()));

        Awaitility.await()
                .pollDelay(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(candidateRepository.count()).isEqualTo(countAfterFirstDelivery));
    }

    private KafkaTemplate<String, String> rawJsonProducer() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(KAFKA.getBootstrapServers());
        var factory = new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer());
        return new KafkaTemplate<>(factory);
    }

    private String sampleEventJson(String candidateId, String parsedAt, String eventId) {
        return """
                {
                  "eventId": "%s",
                  "candidateId": "%s",
                  "parsedAt": "%s",
                  "name": "Тестовый Кандидат Кафкович",
                  "position": "java-middle",
                  "posLabel": "Java — ведущий программист",
                  "email": "%s@email.com",
                  "phone": "+996 700 555555",
                  "city": "Бишкек",
                  "telegram": "@kafka_it",
                  "totalExp": "~2 г.",
                  "stack": "Java, Spring Boot, Kafka",
                  "education": "КНУ, Информатика, 2022",
                  "verdict": "FIT",
                  "summary": "Интеграционный тест Kafka-консьюмера.",
                  "criteria": [
                    { "key": "kafka", "result": "OK", "comment": "Проверка идемпотентности" }
                  ],
                  "experience": [
                    { "period": "2023-01 — н.в.", "company": "TestCorp", "title": "Java Developer", "duration": "~2 г." }
                  ],
                  "questions": ["Тестовый вопрос?"]
                }
                """.formatted(eventId, candidateId, parsedAt, candidateId);
    }
}
