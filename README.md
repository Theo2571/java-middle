# Candidate Service

Микросервис управления кандидатами платформы **CV-Scan**. Потребляет событие `cv.parsed` из Kafka
(идемпотентно создаёт кандидата), предоставляет REST API для HR-интерфейса и публикует
`candidate.status.changed` при каждой смене статуса. Контракт API зафиксирован в
[`contract/openapi.yaml`](contract/openapi.yaml).

## Стек

Java 17, Spring Boot 3.5, PostgreSQL, Apache Kafka, Flyway, Gradle, JUnit 5 + Mockito,
Testcontainers, springdoc-openapi.

## Запуск

Нужны Docker (для Postgres/Kafka) и JDK 17+ (либо используйте Gradle-тулчейн — см. ниже).

```bash
# 1. Поднять Postgres и Kafka
docker-compose up -d

# 2. Запустить сервис
./gradlew bootRun
```

Swagger UI: http://localhost:8080/swagger-ui.html
Health-check: http://localhost:8080/actuator/health

При первом старте Flyway применяет миграции и загружает 12 seed-кандидатов с разными
вердиктами/статусами — API можно проверять сразу, без публикации Kafka-событий.

### Сборка и тесты

```bash
./gradlew build   # сборка + все тесты
./gradlew test    # только тесты
```

Интеграционные тесты поднимают реальные Postgres и Kafka через Testcontainers — **для их
запуска нужен работающий Docker**. Модульные тесты (Mockito) Docker не требуют.

### Ручная проверка Kafka

```bash
# Одно событие
printf '%s\n' "$(tr -d '\n' < test-events/cv-parsed-sample.json)" \
  | kcat -P -b localhost:9092 -t cv.parsed -l

# 15 событий, включая намеренный дубль последней строкой
kcat -P -b localhost:9092 -t cv.parsed -l test-events/cv-parsed-bulk.ndjson
```

Проверить публикацию статусов:

```bash
kcat -C -b localhost:9092 -t candidate.status.changed
```

> **Примечание по `test-events/cv-parsed-sample.json`:** в файле опечатка в поле `parsedAt`
> (`"2026-0 -10T09:00:00Z"` — не является валидной датой). Консьюмер не падает на таком
> сообщении: `ErrorHandlingDeserializer` перехватывает ошибку десериализации, логирует и
> пропускает сообщение, не создавая кандидата и не роняя listener. Для проверки happy-path
> использовался `cv-parsed-bulk.ndjson` (там `parsedAt` корректен), либо можно
> подготовить своё событие с исправленной датой.

## Структура проекта

Соответствует структуре, заданной в `TASK.md`
(`controller` / `service` / `messaging` / `repository` / `model` / `dto` / `exception`),
плюс небольшой пакет `config` для Kafka producer bean.

## Принятые решения

- **Бизнес-id вместо surrogate-ключа.** `candidates.id` — строка (как `candidateId` из Kafka:
  `"ivanov"`, `"asanov-bakyt"`), а не UUID/serial. Контракт не описывает `id` для
  `POST /candidates`, поэтому для REST-создания `SlugIdGenerator` транслитерирует имя в
  slug и добавляет случайный суффикс (уникальность гарантируется проверкой + retry).
- **`criteria`/`experience`/`questions` — JSONB-колонки**, а не дочерние таблицы. Это
  read-only, всегда выбираемые вместе с кандидатом вложенные структуры, которые нигде не
  фильтруются по контракту — маппинг через `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6, без
  доп. зависимостей) исключает классическую проблему N+1 на `@OneToMany` по построению.
- **Идемпотентность консьюмера** — ключ `(candidateId, parsedAt)`: проверка
  `existsByIdAndParsedAt` перед вставкой + перехват `DataIntegrityViolationException` как
  страховка от гонки при повторной доставке (т.к. `id` — первичный ключ).
- **`StatusService`** — единая точка изменения статуса: проверяет переход по таблице
  `Map<CandidateStatus, Set<CandidateStatus>>`, пишет историю и публикует Kafka-событие
  через `@TransactionalEventListener(phase = AFTER_COMMIT)` — событие уходит в Kafka только
  после реального коммита транзакции (история/статус не разъедутся с публикацией при откате).
- **Фильтрация** — Spring Data JPA Specifications: `verdict`/`status`/`position`/`search`
  независимо комбинируются через `Specification.allOf(...)`, любой из них может отсутствовать.
- **DTO — Java records** с Bean Validation, совпадающей с контрактом один в один
  (`@Size`, `@Pattern`, `@Email`, `@NotNull`); ручной `CandidateMapper` вместо MapStruct —
  при таком количестве DTO кодогенерация не окупается.
- **Kafka JSON и даты.** У `spring-kafka`-сериализатора по умолчанию свой `ObjectMapper` без
  `JavaTimeModule`-настроек Spring Boot, из-за чего `Instant` сериализовался бы как epoch-число,
  а не ISO-8601 строка, требуемая схемой события. `KafkaProducerConfig` явно строит
  `ProducerFactory`/`KafkaTemplate` поверх Spring-овского `ObjectMapper`, зафиксировано ручной
  проверкой через `kcat`/`kafka-console-consumer`.
- **Testcontainers использует `confluentinc/cp-kafka`**, хотя `docker-compose.yml` (для
  локального запуска) — `apache/kafka`. `KafkaContainer` из testcontainers-java рассчитан на
  layout и лог-вывод именно Confluent-образа; `apache/kafka` не проходит его
  `LogMessageWaitStrategy`. Для локального запуска выбран `apache/kafka` — официальный образ
  без стороннего вендора.

## Что не реализовано (сознательно, за рамками объёма задания)

- DLT (dead-letter topic) для необрабатываемых Kafka-сообщений — помечено как "плюс" в задании.
- Переобработка `cv.parsed` при повторном парсинге того же кандидата с **другим** `parsedAt`
  (апдейт вместо игнорирования) — контракт и тестовые данные описывают только точное
  дублирование `(candidateId, parsedAt)`.
- MDC-корреляция логов по `requestId`.
- Функциональный индекс `LOWER(name)` для ускорения ILIKE-поиска (сейчас `search` работает
  через `LOWER(name) LIKE ?`, использует обычный индекс частично).

## Тесты

- **Модульные** (`StatusServiceTest`, `CvParsedConsumerTest`, Mockito): все допустимые/
  недопустимые переходы состояний, идемпотентность консьюмера на паре из реального дубля
  в `cv-parsed-bulk.ndjson`.
- **Интеграционные** (`CandidateApiIntegrationTest`, `KafkaIntegrationTest`, Testcontainers —
  реальные Postgres + Kafka): комбинированная фильтрация, `409` на дублирующийся email,
  `422` на недопустимый переход, история статусов, создание кандидата из `cv.parsed` и
  игнорирование дубля через реальный брокер.

`./gradlew test` — **26/26 тестов зелёные** (19 модульных + 7 интеграционных, интеграционные
прогнаны против реальных Postgres 16 и Kafka через Testcontainers).

Дополнительно всё REST API и Kafka-flow были вручную прогнаны через `docker-compose` +
`curl`/`kcat` на реальных Postgres/Kafka (не только через тесты) — в процессе найдены и
исправлены два бага (см. коммит `fix: flush updatedAt before responding, use app ObjectMapper
for Kafka JSON dates`).
