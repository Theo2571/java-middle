# Тестовое задание для позиции Java Middle / Strong Middle

Внутреннее название позиции: **Java — ведущий программист**. По уровню ожиданий это **Strong Middle**: разработчик между Middle и Senior, без обязанностей руководителя команды.

## Контекст

Вы реализуете **Candidate Service** — один микросервис в платформе CV-Scan.

Платформа состоит из нескольких сервисов:

```
cv-parser          →  [Kafka: cv.parsed]  →  candidate-service  →  [Kafka: candidate.status.changed]  →  notification-service
(парсит PDF-резюме)                          (ВАШ СЕРВИС)                                                 (не реализуется)
```

Candidate Service выполняет две задачи:
1. Потребляет событие `cv.parsed` из Kafka — создаёт кандидата в БД (идемпотентно).
2. Предоставляет REST API для HR-интерфейса — управление кандидатами и статусами.

Задание демонстрирует типичные задачи Java-разработчика в экосистеме Түндүк: REST API, контракты, миграции БД, тестирование.

---

## Технические требования

### Стек
- **Java 17+**
- **Spring Boot 3.x** (Web, Data JPA, Validation, Kafka)
- **PostgreSQL** (основное хранилище)
- **Apache Kafka** (чтение и публикация сообщений)
- **Flyway** или **Liquibase** (миграции БД)
- **Gradle** (сборка)
- **JUnit 5 + Mockito** (модульные тесты)
- **Testcontainers** (интеграционные тесты — PostgreSQL + Kafka)
- **Springdoc OpenAPI** (документация, `/swagger-ui.html`)

### Что не требуется
- Фронтенд
- Авторизация / аутентификация
- Развёртывание / CI/CD (опционально)

---

## Контракт API

REST API полностью описан в файле **`contract/openapi.yaml`**.

> **Важно.** Контракт зафиксирован — реализовать его строго. Не добавлять поля, не менять имена, не менять коды ошибок. Отклонение от контракта приравнивается к ошибке реализации.

Swagger UI после запуска: `http://localhost:8080/swagger-ui.html`

---

## Задания

### Задание 1. REST API по контракту

Реализовать все эндпоинты из `contract/openapi.yaml`:

| Метод  | Путь                                    | Описание                             |
|--------|-----------------------------------------|--------------------------------------|
| GET    | `/api/v1/candidates`                    | Список с фильтрацией и пагинацией    |
| GET    | `/api/v1/candidates/{id}`               | Один кандидат                        |
| POST   | `/api/v1/candidates`                    | Создание                             |
| PUT    | `/api/v1/candidates/{id}`               | Обновление                           |
| DELETE | `/api/v1/candidates/{id}`               | Удаление                             |
| PATCH  | `/api/v1/candidates/{id}/status`        | Смена статуса                        |
| GET    | `/api/v1/candidates/{id}/status-history`| История статусов                     |

Поведение должно строго соответствовать контракту: HTTP-статусы, структура ответов, формат ошибок, имена полей.

При создании кандидата через REST API статус всегда устанавливается в `NEW`. Обычное обновление кандидата через `PUT /api/v1/candidates/{id}` не меняет статус. Все изменения статуса выполняются только через `PATCH /api/v1/candidates/{id}/status`.

**Критерии оценки:**
- [ ] Все эндпоинты работают и возвращают HTTP-статусы по контракту
- [ ] Формат ошибок (`ErrorResponse`) совпадает с контрактом
- [ ] Фильтрация и пагинация работают корректно с комбинацией параметров
- [ ] Нет полей, которых нет в контракте; нет переименований

---

### Задание 2. Машина состояний

Допустимые переходы:

```
NEW → IN_REVIEW
IN_REVIEW → INVITED
IN_REVIEW → REJECTED
INVITED → APPROVED
INVITED → REJECTED
```

Любой другой переход — `422` с кодом `INVALID_STATUS_TRANSITION` (формат по контракту).

Каждый переход сохраняется в таблице `candidate_status_history`. Эндпоинт `GET /status-history` возвращает историю в порядке убывания даты.

**Критерии оценки:**
- [ ] Допустимые переходы выполняются
- [ ] Недопустимые переходы отклоняются с `422` и правильным телом ошибки
- [ ] Каждый переход записан в историю
- [ ] История отсортирована по убыванию `changedAt`

---

### Задание 3. Kafka: приём и публикация событий

#### Consumer — топик `cv.parsed`

При появлении события сервис создаёт кандидата. Обработка должна быть **идемпотентной**: повторное событие с тем же `candidateId` и `parsedAt` не должно создавать дубль.

Структура входящего события:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "candidateId": "ivanov",
  "parsedAt": "2026-06-04T10:00:00Z",
  "name": "Иванов Иван Иванович",
  "position": "java-middle",
  "posLabel": "Java — ведущий программист",
  "email": "ivanov@email.com",
  "phone": "+996 999 123456",
  "city": "Бишкек",
  "telegram": "@ivanov_dev",
  "totalExp": "~3.5 г.",
  "stack": "Java, Spring Boot, PostgreSQL, Kafka, Docker",
  "education": "КНУ, Информатика, 2020",
  "verdict": "PARTIAL",
  "summary": "Backend-разработчик с опытом Spring Boot 3 года.",
  "criteria": [
    { "key": "java_spring", "result": "OK",      "comment": "Java 17, Spring Boot — есть (3 года)" },
    { "key": "postgres",    "result": "OK",      "comment": "PostgreSQL — есть" },
    { "key": "kafka",       "result": "PARTIAL", "comment": "Kafka — базовое знакомство" },
    { "key": "tests",       "result": "NO",      "comment": "Тесты не упомянуты" }
  ],
  "experience": [
    { "period": "2022-01 — н.в.", "company": "TechCorp", "title": "Java Developer", "duration": "2 г." }
  ],
  "questions": [
    "Kafka — какой опыт работы с партициями и consumer group?",
    "Как тестировали интеграции с БД?"
  ]
}
```

Созданный кандидат получает статус `NEW`.

#### Producer — топик `candidate.status.changed`

При каждой успешной смене статуса публиковать событие:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "candidateId": "ivanov",
  "fromStatus": "NEW",
  "toStatus": "IN_REVIEW",
  "changedAt": "2026-06-04T12:00:00Z"
}
```

`eventId` — уникальный UUID для каждого события. Конфигурация Kafka (bootstrap servers, топики) — только через `application.yml`, не хардкод.

**Критерии оценки:**
- [ ] Consumer читает из `cv.parsed` и создаёт кандидата
- [ ] Повторное событие с тем же `candidateId` + `parsedAt` игнорируется (нет дубля)
- [ ] Producer публикует в `candidate.status.changed` при каждом переходе
- [ ] Структура сообщений соответствует схеме выше
- [ ] Конфигурация через properties, не хардкод

#### Тестовые события

В папке `test-events/` подготовлены данные для ручной проверки:

| Файл | Описание |
|------|----------|
| `cv-parsed-sample.json` | Одно событие для быстрой проверки чтения из Kafka |
| `cv-parsed-bulk.ndjson` | 15 событий (NDJSON, одно на строку): 5 FIT, 6 PARTIAL, 3 NO_FIT + 1 намеренный дубль последней строкой |

Последнее событие в `cv-parsed-bulk.ndjson` — повторная доставка первого кандидата (тот же `candidateId` + `parsedAt`). Сервис должен его проигнорировать.

Публикация через **kcat**:
```bash
# Одно событие
printf '%s\n' "$(tr -d '\n' < test-events/cv-parsed-sample.json)" \
  | kcat -P -b localhost:9092 -t cv.parsed -l

# 15 событий (каждая строка — отдельное сообщение)
kcat -P -b localhost:9092 -t cv.parsed -l test-events/cv-parsed-bulk.ndjson
```

Публикация через **kafka-console-producer** (если kcat не установлен):
```bash
# Одно событие
tr -d '\n' < test-events/cv-parsed-sample.json \
  | kafka-console-producer.sh --bootstrap-server localhost:9092 --topic cv.parsed
```

---

### Задание 4. Миграции БД

- Использовать Flyway или Liquibase — без `spring.jpa.hibernate.ddl-auto=create/update`
- Минимальный набор миграций:

| Файл | Содержание |
|------|------------|
| `V1__create_candidates.sql` | Таблица `candidates` |
| `V2__create_status_history.sql` | Таблица `candidate_status_history`, FK |
| `V3__seed_data.sql` | 10–15 кандидатов с разными вердиктами и статусами |

Начальные данные нужны для проверки REST API (фильтрация, пагинация, поиск) с первого старта сервиса, без предварительной отправки Kafka-событий. В реальной системе новые кандидаты поступают только через чтение `cv.parsed` — начальные данные имитируют уже накопленные записи.

- Индексы создать в миграциях: `email` (unique), `verdict`, `status`, `position`, `created_at`

**Критерии оценки:**
- [ ] Flyway/Liquibase применяется при старте
- [ ] `ddl-auto` не `create` и не `update`
- [ ] Все индексы созданы через миграции
- [ ] Начальные данные присутствуют

---

### Задание 5. Тестирование

**Модульные тесты (Mockito):**
- Логика машины состояний: допустимые и недопустимые переходы
- Идемпотентность обработки Kafka-события: повторное событие не вызывает создание дубля

**Интеграционные тесты (Testcontainers):**
- Поднимать реальный PostgreSQL и Kafka (не H2 и не встроенную заглушку)
- `GET /api/v1/candidates` — комбинация фильтров возвращает правильные результаты
- `POST /api/v1/candidates` с дублирующимся email → `409`
- `PATCH /api/v1/candidates/{id}/status` — допустимый переход → история сохранена
- `PATCH /api/v1/candidates/{id}/status` — недопустимый переход → `422`
- Consumer: сообщение в топик `cv.parsed` → кандидат создан в БД
- Consumer: повторное сообщение → дубль не создан

**Критерии оценки:**
- [ ] Модульные тесты покрывают машину состояний и идемпотентность
- [ ] Интеграционные тесты поднимают реальные PostgreSQL и Kafka
- [ ] Все перечисленные сценарии покрыты
- [ ] Тесты изолированы (`@Transactional` / `@Sql` — нет побочных эффектов)

---

## Структура проекта

Ожидаемая структура:

```
src/
├── main/
│   ├── java/kg/tunduk/cvscan/candidate/
│   │   ├── controller/
│   │   │   └── CandidateController.java
│   │   ├── service/
│   │   │   ├── CandidateService.java
│   │   │   └── StatusService.java
│   │   ├── messaging/
│   │   │   ├── CvParsedConsumer.java
│   │   │   └── StatusChangedProducer.java
│   │   ├── repository/
│   │   │   ├── CandidateRepository.java
│   │   │   └── StatusHistoryRepository.java
│   │   ├── model/
│   │   │   ├── Candidate.java
│   │   │   └── StatusHistory.java
│   │   ├── dto/
│   │   │   ├── CandidateWriteRequest.java
│   │   │   ├── CandidateResponse.java
│   │   │   ├── CandidatePage.java
│   │   │   ├── StatusChangeRequest.java
│   │   │   ├── StatusHistoryEntry.java
│   │   │   └── event/
│   │   │       ├── CvParsedEvent.java
│   │   │       └── StatusChangedEvent.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── CandidateNotFoundException.java
│   │   │   └── InvalidStatusTransitionException.java
│   │   └── CandidateServiceApplication.java
│   └── resources/
│       ├── db/migration/
│       │   ├── V1__create_candidates.sql
│       │   ├── V2__create_status_history.sql
│       │   └── V3__seed_data.sql
│       └── application.yml
└── test/
    ├── java/kg/tunduk/cvscan/candidate/
    │   ├── service/
    │   │   ├── StatusServiceTest.java
    │   │   └── CvParsedConsumerTest.java
    │   └── integration/
    │       ├── CandidateApiIntegrationTest.java
    │       └── KafkaIntegrationTest.java
    └── resources/
        └── application-test.yml
```

---

## Что оценивается

### Обязательно
- Соответствие REST API контракту (`contract/openapi.yaml`) — точные имена полей, статусы, формат ошибок
- Kafka: чтение и публикация сообщений, идемпотентность обработки
- Миграции БД (Flyway/Liquibase)
- Тесты: модульные и интеграционные с Testcontainers
- Разделение ответственности: controller / service / messaging / repository

### Будет плюсом
- Docker Compose для локального запуска (PostgreSQL + Kafka)
- Spring Data Specifications или Querydsl для фильтрации
- MDC для корреляции логов по запросам
- Топик необработанных сообщений (DLT) для сообщений, которые не удалось обработать
- `@EntityGraph` / join fetch — нет N+1

---

## Критерии оценки

| Критерий | Junior | Middle | Strong Middle |
|----------|--------|--------|---------------|
| **Контракт** | Отклоняется от контракта | Реализует точно | Не добавляет лишнего, стабильные коды ошибок |
| **Kafka** | Не реализована | Чтение и публикация работают | Идемпотентность, DLT, повторы |
| **БД / ORM** | `ddl-auto=create` | Flyway, индексы | Нет N+1, оптимизированные запросы |
| **Тесты** | Нет или только успешный сценарий | Testcontainers, пограничные случаи | Покрытие >70%, изоляция |
| **Архитектура** | Всё в контроллере | Controller / Service / Messaging | Чистые интерфейсы, минимальные зависимости |

---

## Время выполнения

Рекомендуемое время: **8–12 часов**

Дедлайн сдачи: **7 дней** с момента получения задания

---

## Формат сдачи

1. **Репозиторий GitHub/GitLab** (публичный или invite)
2. **README.md** с:
   - Инструкциями по запуску
   - Описанием принятых решений
   - Что не успели и почему
3. **Коммиты** — осмысленная история, не один «done»

---

## Запуск и проверка

```bash
# Сборка и тесты
./gradlew build

# Только тесты
./gradlew test

# Запуск (нужны PostgreSQL и Kafka)
./gradlew bootRun

# Если есть Docker Compose
docker-compose up -d
./gradlew bootRun
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Вопросы

Если возникли вопросы по заданию — задавайте до начала. Если в контракте что-то кажется неоднозначным, уточняйте: в реальной работе это именно то, что нужно делать.
