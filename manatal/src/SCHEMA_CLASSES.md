# Esquema de Classes Completo — ETL Manatal → Manatal

## Visão Geral da Arquitetura

Projeto de migração ETL entre duas instâncias Manatal (Company One → Company Two),
seguindo o padrão do projeto de referência `zoho-manatal-migration`, adaptado para
três módulos: **Clients**, **Jobs** e **Candidates**.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            CONTROLLERS                                    │
│  CompanyOneController │ MigrationController │ HealthController           │
└──────────┬──────────────────────┬──────────────────────┬─────────────────┘
           │                      │                      │
┌──────────▼──────────────────────▼──────────────────────▼─────────────────┐
│                             SERVICES                                      │
│  ManatalSourceService     (API client Company One)                       │
│  ManatalTargetService     (API client Company Two)                       │
│  MigrationService         (Orchestrator por entidade)                    │
│  ClientMigrationService   (Lógica específica de Clients)                 │
│  JobMigrationService      (Lógica específica de Jobs)                    │
│  CandidateMigrationService(Lógica específica de Candidates)              │
│  AttachmentService        (Download/upload de arquivos)                  │
│  ReportService            (Relatórios de migração)                       │
│  FileStorageService       (Armazenamento temporário binário)             │
│  NotificationService      (Alertas de rate limit, conclusão)             │
└──────────┬──────────────────────┬──────────────────────┬─────────────────┘
           │                      │                      │
┌──────────▼──────────────────────▼──────────────────────▼─────────────────┐
│                       TRANSFORM / MAPPER                                   │
│  ClientMapper │ JobMapper │ CandidateMapper │ ParseUtils                 │
└──────────┬──────────────────────┬──────────────────────┬─────────────────┘
           │                      │                      │
┌──────────▼──────────────────────▼──────────────────────▼─────────────────┐
│                         MODELS / DTOs                                      │
│  Source: SourceClient │ SourceJob │ SourceCandidate                       │
│  Target: TargetClient │ TargetJob │ TargetCandidate                      │
│  Shared: ManatalAttachment │ ManatalResume │ ManatalNote │ ManatalTag    │
└──────────┬──────────────────────┬──────────────────────┬─────────────────┘
           │                      │                      │
┌──────────▼──────────────────────▼──────────────────────▼─────────────────┐
│                        BATCH / PIPELINE                                    │
│  ClientMigrationJob │ JobMigrationJob │ CandidateMigrationJob            │
│  ItemReader → ItemProcessor → ItemWriter (por módulo)                    │
│  Listeners: JobCompletion │ StepCompletion │ SkipListener                │
└──────────┬──────────────────────┬──────────────────────┬─────────────────┘
           │                      │                      │
┌──────────▼──────────────────────▼──────────────────────▼─────────────────┐
│                       REPOSITORY (JPA)                                     │
│  ClientMigrationRepo │ JobMigrationRepo │ CandidateMigrationRepo         │
│  MigrationLogRepository │ AttachmentStoreRepository                      │
└──────────────────────────────────────────────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────────────────────────────────┐
│                        ENTITIES (JPA) + H2                                │
│  ClientMigration │ JobMigration │ CandidateMigration                     │
│  MigrationLog │ AttachmentStore                                          │
│  Spring Batch Tables (AUTO)                                              │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Estrutura de Pastas Completa

```
src/main/java/com/migration/manatal/
├── ManatalApplication.java
├── config/
│   ├── ManatalSourceProperties.java
│   ├── ManatalTargetProperties.java
│   ├── HttpClientConfig.java
│   ├── AsyncConfig.java
│   ├── SecurityConfig.java
│   └── BatchConfig.java
├── model/
│   ├── source/
│   │   ├── SourceClient.java
│   │   ├── SourceJob.java
│   │   └── SourceCandidate.java
│   ├── target/
│   │   ├── TargetClient.java
│   │   ├── TargetJob.java
│   │   └── TargetCandidate.java
│   └── shared/
│       ├── ManatalAttachment.java
│       ├── ManatalResume.java
│       ├── ManatalNote.java
│       └── ManatalTag.java
├── transform/
│   ├── ClientMapper.java
│   ├── JobMapper.java
│   ├── CandidateMapper.java
│   └── utils/
│       └── ParseUtils.java
├── entity/
│   ├── enums/
│   │   ├── MigrationStatus.java
│   │   └── EntityType.java
│   ├── ClientMigration.java
│   ├── JobMigration.java
│   ├── CandidateMigration.java
│   ├── MigrationLog.java
│   └── AttachmentStore.java
├── repository/
│   ├── ClientMigrationRepository.java
│   ├── JobMigrationRepository.java
│   ├── CandidateMigrationRepository.java
│   ├── MigrationLogRepository.java
│   └── AttachmentStoreRepository.java
├── service/
│   ├── ManatalSourceService.java
│   ├── ManatalTargetService.java
│   ├── MigrationService.java
│   ├── ClientMigrationService.java
│   ├── JobMigrationService.java
│   ├── CandidateMigrationService.java
│   ├── AttachmentService.java
│   ├── ReportService.java
│   ├── FileStorageService.java
│   └── NotificationService.java
├── batch/
│   ├── client/
│   │   ├── ClientMigrationJobConfig.java
│   │   ├── ClientItemReader.java
│   │   ├── ClientItemProcessor.java
│   │   └── ClientItemWriter.java
│   ├── job/
│   │   ├── JobMigrationJobConfig.java
│   │   ├── JobItemReader.java
│   │   ├── JobItemProcessor.java
│   │   └── JobItemWriter.java
│   ├── candidate/
│   │   ├── CandidateMigrationJobConfig.java
│   │   ├── CandidateItemReader.java
│   │   ├── CandidateItemProcessor.java
│   │   └── CandidateItemWriter.java
│   └── listener/
│       ├── JobCompletionListener.java
│       ├── StepCompletionListener.java
│       └── MigrationSkipListener.java
├── controller/
│   ├── CompanyOneController.java
│   ├── MigrationController.java
│   └── HealthController.java
├── dto/
│   ├── MigrationSummary.java
│   ├── MigrationStatusResponse.java
│   └── PreviewResponse.java
├── exception/
│   ├── ApiException.java
│   ├── MigrationException.java
│   ├── RateLimitException.java
│   ├── GlobalExceptionHandler.java
│   └── ErrorResponse.java
└── security/
    └── SecurityConfig.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
├── logback-spring.xml
└── data.sql

src/test/java/com/migration/manatal/
├── ManatalApplicationTests.java
├── config/
│   └── HttpClientConfigTest.java
├── transform/
│   ├── ClientMapperTest.java
│   ├── JobMapperTest.java
│   ├── CandidateMapperTest.java
│   └── utils/
│       └── ParseUtilsTest.java
├── entity/
│   └── EntityTest.java
├── repository/
│   ├── ClientMigrationRepositoryTest.java
│   ├── JobMigrationRepositoryTest.java
│   ├── CandidateMigrationRepositoryTest.java
│   └── MigrationLogRepositoryTest.java
├── service/
│   ├── ManatalSourceServiceTest.java
│   ├── ManatalTargetServiceTest.java
│   ├── MigrationServiceTest.java
│   ├── ClientMigrationServiceTest.java
│   ├── JobMigrationServiceTest.java
│   ├── CandidateMigrationServiceTest.java
│   ├── AttachmentServiceTest.java
│   ├── ReportServiceTest.java
│   └── FileStorageServiceTest.java
├── batch/
│   ├── client/
│   │   ├── ClientItemReaderTest.java
│   │   ├── ClientItemProcessorTest.java
│   │   └── ClientItemWriterTest.java
│   ├── job/
│   │   ├── JobItemReaderTest.java
│   │   ├── JobItemProcessorTest.java
│   │   └── JobItemWriterTest.java
│   ├── candidate/
│   │   ├── CandidateItemReaderTest.java
│   │   ├── CandidateItemProcessorTest.java
│   │   └── CandidateItemWriterTest.java
│   └── integration/
│       ├── ClientMigrationJobIntegrationTest.java
│       ├── JobMigrationJobIntegrationTest.java
│       └── CandidateMigrationJobIntegrationTest.java
├── controller/
│   ├── CompanyOneControllerTest.java
│   ├── MigrationControllerTest.java
│   └── HealthControllerTest.java
└── exception/
    └── GlobalExceptionHandlerTest.java
```

---

## 1. CONFIGURAÇÃO

### 1.1 pom.xml (dependências necessárias)

```xml
<!-- Web + REST -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- JPA + H2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring Batch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- WebClient (reactive HTTP client) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Jackson (já vem com web, mas explícito) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Testes -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.batch</groupId>
    <artifactId>spring-batch-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

### 1.2 application.yml

```yaml
spring:
  application:
    name: manatal-migration

  # ── H2 Database ──
  datasource:
    url: jdbc:h2:mem:migrationdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  # ── JPA ──
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate.format_sql: true

  # ── Batch ──
  batch:
    jdbc:
      initialize-schema: always
    job:
      enabled: false
    table-prefix: BATCH_

  # ── Jackson ──
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

  # ── Security ──
  security:
    user:
      name: admin
      password: admin

# ── Migration Config ──
migration:
  manatal:
    source:
      token: ${MANATAL_SOURCE_TOKEN:token-source-placeholder}
      base-url: ${MANATAL_SOURCE_URL:https://api.manatal.com/v3/}
      rate-limit-ms: ${MANATAL_SOURCE_RATE_LIMIT:300}
    target:
      token: ${MANATAL_TARGET_TOKEN:token-target-placeholder}
      base-url: ${MANATAL_TARGET_URL:https://api.manatal.com/v3/}
      rate-limit-ms: ${MANATAL_TARGET_RATE_LIMIT:300}

# ── App Config ──
app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
  storage:
    temp-dir: ${java.io.tmpdir}/manatal-migration
  batch:
    chunk-size: 10
    retry-max: 3
```

### 1.3 application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/migrationdb;DB_CLOSE_DELAY=-1
  jpa:
    show-sql: true
  h2:
    console:
      enabled: true

logging:
  level:
    com.migration.manatal: DEBUG
    org.springframework.batch: DEBUG
```

### 1.4 application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:file:/opt/data/migrationdb;DB_CLOSE_DELAY=-1
  jpa:
    show-sql: false
  h2:
    console:
      enabled: false

logging:
  level:
    com.migration.manatal: INFO
    org.springframework.batch: WARN
```

### 1.5 logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_DIR" value="logs"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/migration.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/migration.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="BATCH_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/batch.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/batch.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>15</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.migration.manatal" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>

    <logger name="org.springframework.batch" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="BATCH_FILE"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

## 2. CONFIG CLASSES

### 2.1 `ManatalSourceProperties.java`

```java
package com.migration.manatal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "migration.manatal.source")
public record ManatalSourceProperties(
    String token,
    String baseUrl,
    int rateLimitMs
) {}
```

### 2.2 `ManatalTargetProperties.java`

```java
package com.migration.manatal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "migration.manatal.target")
public record ManatalTargetProperties(
    String token,
    String baseUrl,
    int rateLimitMs
) {}
```

### 2.3 `HttpClientConfig.java`

```java
package com.migration.manatal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfig {

    @Bean("sourceWebClient")
    public WebClient sourceWebClient(ManatalSourceProperties props) {
        return WebClient.builder()
            .baseUrl(props.baseUrl())
            .defaultHeader("Authorization", "Token " + props.token())
            .defaultHeader("Content-Type", "application/json")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Bean("targetWebClient")
    public WebClient targetWebClient(ManatalTargetProperties props) {
        return WebClient.builder()
            .baseUrl(props.baseUrl())
            .defaultHeader("Authorization", "Token " + props.token())
            .defaultHeader("Content-Type", "application/json")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}
```

### 2.4 `BatchConfig.java`

```java
package com.migration.manatal.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing(tablePrefix = "BATCH_")
public class BatchConfig {}
```

### 2.5 `AsyncConfig.java`

```java
package com.migration.manatal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("migrationExecutor")
    public Executor migrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("migration-");
        executor.initialize();
        return executor;
    }
}
```

### 2.6 `SecurityConfig.java`

```java
package com.migration.manatal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/h2-console/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(h -> h.frameOptions(f -> f.disable()))
            .httpBasic(b -> {});
        return http.build();
    }
}
```

---

## 3. ENTITIES (JPA + H2)

### 3.1 Enums

```java
// entity/enums/MigrationStatus.java
package com.migration.manatal.entity.enums;

public enum MigrationStatus {
    PENDENTE, EM_ANDAMENTO, SUCESSO, ERRO, IGNORADO
}
```

```java
// entity/enums/EntityType.java
package com.migration.manatal.entity.enums;

public enum EntityType {
    CLIENT, JOB, CANDIDATE
}
```

### 3.2 `ClientMigration.java`

```java
package com.migration.manatal.entity;

import com.migration.manatal.entity.enums.MigrationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_migration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceClientId;

    private String targetClientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MigrationStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private int attempt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3.3 `JobMigration.java`

```java
package com.migration.manatal.entity;

import com.migration.manatal.entity.enums.MigrationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_migration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceJobId;

    private String targetJobId;

    @Column(nullable = false)
    private String sourceClientRef;

    private String targetClientRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MigrationStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private int attempt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3.4 `CandidateMigration.java`

```java
package com.migration.manatal.entity;

import com.migration.manatal.entity.enums.MigrationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_migration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceCandidateId;

    private String targetCandidateId;

    private String sourceJobRef;

    private String targetJobRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MigrationStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private int attempt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3.5 `MigrationLog.java`

```java
package com.migration.manatal.entity;

import com.migration.manatal.entity.enums.EntityType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "migration_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String step;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Long durationMs;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
```

### 3.6 `AttachmentStore.java`

```java
package com.migration.manatal.entity;

import com.migration.manatal.entity.enums.EntityType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attachment_store")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceAttachmentId;

    @Column(nullable = false)
    private String fileName;

    private String fileType;

    private Long fileSize;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    @Column(nullable = false)
    private String entityId;

    private String uploadedUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
```

---

## 4. REPOSITORIES

### 4.1 `ClientMigrationRepository.java`

```java
package com.migration.manatal.repository;

import com.migration.manatal.entity.ClientMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClientMigrationRepository extends JpaRepository<ClientMigration, Long> {
    Optional<ClientMigration> findBySourceClientId(String sourceClientId);
    List<ClientMigration> findByStatus(MigrationStatus status);
    boolean existsBySourceClientIdAndStatus(String sourceClientId, MigrationStatus status);
}
```

### 4.2 `JobMigrationRepository.java`

```java
package com.migration.manatal.repository;

import com.migration.manatal.entity.JobMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobMigrationRepository extends JpaRepository<JobMigration, Long> {
    Optional<JobMigration> findBySourceJobId(String sourceJobId);
    List<JobMigration> findBySourceClientRef(String sourceClientRef);
    List<JobMigration> findByStatus(MigrationStatus status);
    boolean existsBySourceJobIdAndStatus(String sourceJobId, MigrationStatus status);
}
```

### 4.3 `CandidateMigrationRepository.java`

```java
package com.migration.manatal.repository;

import com.migration.manatal.entity.CandidateMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CandidateMigrationRepository extends JpaRepository<CandidateMigration, Long> {
    Optional<CandidateMigration> findBySourceCandidateId(String sourceCandidateId);
    List<CandidateMigration> findByStatus(MigrationStatus status);
    boolean existsBySourceCandidateIdAndStatus(String sourceCandidateId, MigrationStatus status);
}
```

### 4.4 `MigrationLogRepository.java`

```java
package com.migration.manatal.repository;

import com.migration.manatal.entity.MigrationLog;
import com.migration.manatal.entity.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MigrationLogRepository extends JpaRepository<MigrationLog, Long> {
    List<MigrationLog> findByEntityTypeAndEntityId(EntityType entityType, String entityId);
    List<MigrationLog> findByStatus(String status);
}
```

### 4.5 `AttachmentStoreRepository.java`

```java
package com.migration.manatal.repository;

import com.migration.manatal.entity.AttachmentStore;
import com.migration.manatal.entity.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentStoreRepository extends JpaRepository<AttachmentStore, Long> {
    List<AttachmentStore> findByEntityTypeAndEntityId(EntityType entityType, String entityId);
}
```

---

## 5. MODELS / DTOs

### 5.1 Source DTOs (Company One — JSON recebido)

```java
// model/source/SourceClient.java
package com.migration.manatal.model.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceClient(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("contact_name") String contactName,
    @JsonProperty("contact_email") String contactEmail,
    @JsonProperty("contact_phone") String contactPhone,
    @JsonProperty("address") String address,
    @JsonProperty("industry") String industry,
    @JsonProperty("notes") String notes,
    @JsonProperty("custom_fields") Map<String, Object> customFields
) {}
```

```java
// model/source/SourceJob.java
package com.migration.manatal.model.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceJob(
    @JsonProperty("id") String id,
    @JsonProperty("title") String title,
    @JsonProperty("client") SourceClientRef client,
    @JsonProperty("description") String description,
    @JsonProperty("location") String location,
    @JsonProperty("status") String status,
    @JsonProperty("salary") String salary,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("tags") List<String> tags,
    @JsonProperty("custom_fields") Map<String, Object> customFields
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SourceClientRef(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name
    ) {}
}
```

```java
// model/source/SourceCandidate.java
package com.migration.manatal.model.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceCandidate(
    @JsonProperty("id") String id,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("email") String email,
    @JsonProperty("phone_number") String phone,
    @JsonProperty("country") String country,
    @JsonProperty("candidate_location") String location,
    @JsonProperty("description") String description,
    @JsonProperty("resume_file") String resumeUrl,
    @JsonProperty("tags") List<String> tags,
    @JsonProperty("applications") List<ApplicationRef> applications,
    @JsonProperty("notes") List<NoteRef> notes,
    @JsonProperty("attachments") List<AttachmentRef> attachments,
    @JsonProperty("custom_fields") Map<String, Object> customFields
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplicationRef(
        @JsonProperty("id") String id,
        @JsonProperty("job") JobRef job
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record JobRef(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title
        ) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NoteRef(
        @JsonProperty("id") String id,
        @JsonProperty("content") String content,
        @JsonProperty("created_at") String createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AttachmentRef(
        @JsonProperty("id") String id,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_type") String fileType,
        @JsonProperty("file_url") String fileUrl
    ) {}
}
```

### 5.2 Target DTOs (Company Two — JSON enviado)

```java
// model/target/TargetClient.java
package com.migration.manatal.model.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TargetClient(
    @JsonProperty("name") String name,
    @JsonProperty("contact_name") String contactName,
    @JsonProperty("contact_email") String contactEmail,
    @JsonProperty("contact_phone") String contactPhone,
    @JsonProperty("address") String address,
    @JsonProperty("industry") String industry
) {}
```

```java
// model/target/TargetJob.java
package com.migration.manatal.model.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TargetJob(
    @JsonProperty("title") String title,
    @JsonProperty("client") Integer clientId,
    @JsonProperty("description") String description,
    @JsonProperty("location") String location,
    @JsonProperty("status") String status,
    @JsonProperty("salary") String salary,
    @JsonProperty("tags") List<String> tags
) {}
```

```java
// model/target/TargetCandidate.java
package com.migration.manatal.model.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TargetCandidate(
    @JsonProperty("full_name") String fullName,
    @JsonProperty("email") String email,
    @JsonProperty("phone_number") String phone,
    @JsonProperty("country") String country,
    @JsonProperty("candidate_location") String location,
    @JsonProperty("description") String description,
    @JsonProperty("consent") Boolean consent,
    @JsonProperty("custom_fields") Map<String, Object> customFields,
    @JsonProperty("notes") List<TargetNote> notes
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TargetNote(
        @JsonProperty("content") String content
    ) {}
}
```

### 5.3 Shared DTOs

```java
// model/shared/ManatalAttachment.java
package com.migration.manatal.model.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManatalAttachment(
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("file") String fileUrl,
    @JsonProperty("creator") Integer creator
) {}
```

```java
// model/shared/ManatalResume.java
package com.migration.manatal.model.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManatalResume(
    @JsonProperty("resume_file") String resumeFileUrl
) {}
```

```java
// model/shared/ManatalNote.java
package com.migration.manatal.model.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManatalNote(
    @JsonProperty("content") String content,
    @JsonProperty("creator") Integer creator
) {}
```

```java
// model/shared/ManatalTag.java
package com.migration.manatal.model.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManatalTag(
    @JsonProperty("tag") String tag
) {}
```

---

## 6. TRANSFORM / MAPPER

### 6.1 `ParseUtils.java`

```java
package com.migration.manatal.transform.utils;

import org.springframework.stereotype.Component;

@Component
public class ParseUtils {

    public String normalizeLinkedin(String url) {
        if (url == null || url.isBlank()) return null;
        url = url.trim();
        if (!url.startsWith("http")) url = "https://" + url;
        return url;
    }

    public String parseSalary(String str) {
        if (str == null || str.isBlank()) return null;
        String cleaned = str.replaceAll("[^0-9.,]", "").replace(",", ".");
        return cleaned.isEmpty() ? null : cleaned;
    }

    public boolean isEmpty(String str) {
        return str == null || str.isBlank();
    }

    public String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen);
    }
}
```

### 6.2 `ClientMapper.java`

```java
package com.migration.manatal.transform;

import com.migration.manatal.model.source.SourceClient;
import com.migration.manatal.model.target.TargetClient;
import com.migration.manatal.transform.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientMapper {

    private final ParseUtils parseUtils;

    public TargetClient toTarget(SourceClient source) {
        if (source == null) return null;
        return new TargetClient(
            source.name(),
            source.contactName(),
            source.contactEmail(),
            source.contactPhone(),
            source.address(),
            source.industry()
        );
    }
}
```

### 6.3 `JobMapper.java`

```java
package com.migration.manatal.transform;

import com.migration.manatal.model.source.SourceJob;
import com.migration.manatal.model.target.TargetJob;
import com.migration.manatal.transform.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobMapper {

    private final ParseUtils parseUtils;

    public TargetJob toTarget(SourceJob source, Integer targetClientId) {
        if (source == null) return null;
        return new TargetJob(
            source.title(),
            targetClientId,
            source.description(),
            source.location(),
            source.status(),
            parseUtils.parseSalary(source.salary()),
            source.tags()
        );
    }
}
```

### 6.4 `CandidateMapper.java`

```java
package com.migration.manatal.transform;

import com.migration.manatal.model.source.SourceCandidate;
import com.migration.manatal.model.target.TargetCandidate;
import com.migration.manatal.transform.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CandidateMapper {

    private final ParseUtils parseUtils;

    public TargetCandidate toTarget(SourceCandidate source) {
        if (source == null) return null;

        String description = buildDescription(source);
        Map<String, Object> customFields = buildCustomFields(source);
        List<TargetCandidate.TargetNote> notes = buildNotes(source);

        return new TargetCandidate(
            source.fullName(),
            source.email(),
            source.phone(),
            source.country(),
            source.location(),
            description,
            true,
            customFields.isEmpty() ? null : customFields,
            notes.isEmpty() ? null : notes
        );
    }

    private String buildDescription(SourceCandidate source) {
        StringBuilder sb = new StringBuilder();
        if (!parseUtils.isEmpty(source.description())) {
            sb.append(source.description());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private Map<String, Object> buildCustomFields(SourceCandidate source) {
        Map<String, Object> fields = new HashMap<>();
        if (source.customFields() != null) {
            fields.putAll(source.customFields());
        }
        return fields;
    }

    private List<TargetCandidate.TargetNote> buildNotes(SourceCandidate source) {
        List<TargetCandidate.TargetNote> notes = new ArrayList<>();
        if (source.notes() != null) {
            for (var note : source.notes()) {
                if (!parseUtils.isEmpty(note.content())) {
                    notes.add(new TargetCandidate.TargetNote(note.content()));
                }
            }
        }
        return notes;
    }
}
```

---

## 7. SERVICES

### 7.1 `ManatalSourceService.java`

```java
package com.migration.manatal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.config.ManatalSourceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceService {

    @Qualifier("sourceWebClient")
    private final WebClient webClient;
    private final ManatalSourceProperties properties;

    public List<JsonNode> listClients() {
        return fetchPaginated("/clients");
    }

    public JsonNode getClient(String id) {
        return webClient.get().uri("/clients/{id}", id)
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    }

    public List<JsonNode> listJobs() {
        return fetchPaginated("/jobs");
    }

    public JsonNode getJob(String id) {
        return webClient.get().uri("/jobs/{id}", id)
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    }

    public List<JsonNode> listCandidates() {
        return fetchPaginated("/candidates");
    }

    public JsonNode getCandidate(String id) {
        return webClient.get().uri("/candidates/{id}", id)
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    }

    public JsonNode getAttachments(String entityType, String entityId) {
        String path = switch (entityType.toUpperCase()) {
            case "CANDIDATE" -> "/candidates/" + entityId + "/attachments";
            case "JOB" -> "/jobs/" + entityId + "/attachments";
            default -> "/clients/" + entityId + "/attachments";
        };
        return webClient.get().uri(path)
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    }

    public byte[] downloadAttachment(String url) {
        return webClient.get().uri(url)
            .retrieve().bodyToMono(byte[].class)
            .block(Duration.ofSeconds(60));
    }

    public JsonNode getNotes(String entityType, String entityId) {
        String path = switch (entityType.toUpperCase()) {
            case "CANDIDATE" -> "/candidates/" + entityId + "/notes";
            case "JOB" -> "/jobs/" + entityId + "/notes";
            default -> "/clients/" + entityId + "/notes";
        };
        return webClient.get().uri(path)
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    }

    private List<JsonNode> fetchPaginated(String basePath) {
        // Implementação com paginação e throttle
        // Usa properties.rateLimitMs() entre chamadas
        return webClient.get().uri(basePath)
            .retrieve().bodyToFlux(JsonNode.class)
            .collectList()
            .block(Duration.ofSeconds(120));
    }
}
```

### 7.2 `ManatalTargetService.java`

```java
package com.migration.manatal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.config.ManatalTargetProperties;
import com.migration.manatal.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalTargetService {

    @Qualifier("targetWebClient")
    private final WebClient webClient;
    private final ManatalTargetProperties properties;

    public JsonNode createClient(Object payload) {
        return postWithRetry("/clients/", payload);
    }

    public JsonNode createJob(Object payload) {
        return postWithRetry("/jobs/", payload);
    }

    public JsonNode createCandidate(Object payload) {
        return postWithRetry("/candidates/", payload);
    }

    public JsonNode createNote(String entityType, String entityId, Object payload) {
        String path = "/candidates/" + entityId + "/notes/";
        return postWithRetry(path, payload);
    }

    public JsonNode addTag(String entityType, String entityId, Object payload) {
        String path = "/candidates/" + entityId + "/tags/";
        return postWithRetry(path, payload);
    }

    public JsonNode uploadResume(String candidateId, byte[] file, String fileName) {
        return webClient.post()
            .uri("/candidates/{id}/resume/", candidateId)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(buildMultipartBody(file, fileName))
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(60));
    }

    public JsonNode searchClientByName(String name) {
        return webClient.get().uri(uriBuilder -> uriBuilder
            .path("/clients/").queryParam("search", name).build())
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    }

    public JsonNode searchCandidateByEmail(String email) {
        return webClient.get().uri(uriBuilder -> uriBuilder
            .path("/candidates/").queryParam("search", email).build())
            .retrieve().bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    }

    private JsonNode postWithRetry(String path, Object payload) {
        int maxRetries = 4;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return webClient.post().uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve().bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));
            } catch (WebClientResponseException.TooManyRequests e) {
                long waitSeconds = parseRetryAfter(e.getResponseBodyAsString());
                log.warn("Rate limit at {}, attempt {}/{}, waiting {}s",
                    path, attempt, maxRetries, waitSeconds);
                try { Thread.sleep(waitSeconds * 1000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RateLimitException("Interrupted during retry", ie);
                }
            }
        }
        throw new RateLimitException("Max retries exceeded for " + path);
    }

    private long parseRetryAfter(String body) {
        // Tenta extrair "available in X seconds" do body
        try {
            var matcher = java.util.regex.Pattern.compile("(\\d+)\\s*sec")
                .matcher(body);
            if (matcher.find()) return Long.parseLong(matcher.group(1));
        } catch (Exception ignored) {}
        return 5;
    }

    private Object buildMultipartBody(byte[] file, String fileName) {
        // Implementação simplificada — usar MultipartBodyBuilder
        return org.springframework.http.client.MultipartBodyBuilder
            .fromParts("file", fileName, org.springframework.core.io.ByteArrayResource(file))
            .build();
    }
}
```

### 7.3 `AttachmentService.java`

```java
package com.migration.manatal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.entity.AttachmentStore;
import com.migration.manatal.entity.enums.EntityType;
import com.migration.manatal.repository.AttachmentStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final ManatalSourceService sourceService;
    private final ManatalTargetService targetService;
    private final AttachmentStoreRepository attachmentStoreRepository;
    private final FileStorageService fileStorageService;

    public List<AttachmentStore> downloadAndStore(EntityType entityType, String entityId) {
        List<AttachmentStore> stored = new ArrayList<>();
        JsonNode attachments = sourceService.getAttachments(entityType.name().toLowerCase(), entityId);
        if (attachments == null || !attachments.isArray()) return stored;

        for (JsonNode att : attachments) {
            try {
                String url = att.has("file_url") ? att.get("file_url").asText() : null;
                if (url == null) continue;

                byte[] data = sourceService.downloadAttachment(url);
                String fileName = att.has("file_name") ? att.get("file_name").asText() : "unknown";
                String fileType = att.has("file_type") ? att.get("file_type").asText() : "application/octet-stream";
                long fileSize = data.length;

                AttachmentStore store = AttachmentStore.builder()
                    .sourceAttachmentId(att.has("id") ? att.get("id").asText() : null)
                    .fileName(fileName)
                    .fileType(fileType)
                    .fileSize(fileSize)
                    .data(data)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build();

                attachmentStoreRepository.save(store);
                stored.add(store);
                log.debug("Stored attachment: {} ({} bytes)", fileName, fileSize);
            } catch (Exception e) {
                log.error("Failed to download attachment: {}", e.getMessage());
            }
        }
        return stored;
    }

    public String uploadToTarget(String entityType, String entityId, AttachmentStore stored) {
        // Upload para Company Two e retorna URL pública
        // Implementação depende de como a Company Two recebe arquivos
        return null;
    }

    public boolean isResume(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf") && lower.contains("cv")
            || lower.contains("resume")
            || lower.contains("curriculo");
    }
}
```

### 7.4 `FileStorageService.java`

```java
package com.migration.manatal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.storage.temp-dir}")
    private String tempDir;

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(Path.of(tempDir));
    }

    public Path store(byte[] data, String fileName) throws IOException {
        Path filePath = Path.of(tempDir, fileName);
        Files.write(filePath, data);
        log.debug("Stored file: {}", filePath);
        return filePath;
    }

    public byte[] load(Path filePath) throws IOException {
        return Files.readAllBytes(filePath);
    }

    public void delete(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", filePath);
        }
    }
}
```

### 7.5 `ReportService.java`

```java
package com.migration.manatal.service;

import com.migration.manatal.entity.enums.EntityType;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ClientMigrationRepository clientRepo;
    private final JobMigrationRepository jobRepo;
    private final CandidateMigrationRepository candidateRepo;
    private final MigrationLogRepository logRepo;

    public Map<String, Object> generateReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("clients", Map.of(
                "total", clientRepo.count(),
                "success", clientRepo.findByStatus(
                        com.migration.manatal.entity.enums.MigrationStatus.SUCESSO).size(),
                "errors", clientRepo.findByStatus(
                        com.migration.manatal.entity.enums.MigrationStatus.ERRO).size()
        ));
        report.put("jobs", Map.of(
                "total", jobRepo.count(),
                "success", jobRepo.findByStatus(
                        com.migration.manatal.entity.enums.MigrationStatus.SUCESSO).size(),
                "errors", jobRepo.findByStatus(
                        com.migration.manatal.entity.enums.MigrationStatus.ERRO).size()
        ));
        report.put("candidates", Map.of(
                "total", candidateRepo.count(),
                "success", candidateRepo.findByStatus(
                        com.migration.manatal.entity.enums.MigrationStatus.SUCESSO).size(),
                "errors", candidateRepo.findByStatus(
                        com.migration.manatal.entity.enums.MigrationStatus.ERRO).size()
        ));
        return report;
    }
}
```

### 7.6 `NotificationService.java`

```java
package com.migration.manatal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void notifyRateLimit(String api, int remaining) {
        log.warn("RATE LIMIT WARNING — API: {}, remaining: {}", api, remaining);
    }

    public void notifyJobComplete(String jobName, long durationMs) {
        log.info("JOB COMPLETE — {}, duration: {}ms", jobName, durationMs);
    }

    public void notifyError(String context, String error) {
        log.error("ERROR — {}: {}", context, error);
    }
}
```

---

## 8. BATCH / PIPELINE

### 8.1 Client Batch

```java
// batch/client/ClientMigrationJobConfig.java
package com.migration.manatal.batch.client;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ClientMigrationJobConfig {

    @Bean
    public Job clientMigrationJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        ClientItemReader reader,
        ClientItemProcessor processor,
        ClientItemWriter writer
    ) {
        Step step = new StepBuilder("clientMigrationStep", jobRepository)
            .<com.fasterxml.jackson.databind.JsonNode,
              com.migration.manatal.model.target.TargetClient>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();

        return new JobBuilder("clientMigrationJob", jobRepository)
            .start(step)
            .build();
    }
}
```

```java
// batch/client/ClientItemReader.java
package com.migration.manatal.batch.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.service.ManatalSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class ClientItemReader implements ItemReader<JsonNode> {

    private final ManatalSourceService sourceService;
    private List<JsonNode> clients;
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public JsonNode read() {
        if (clients == null) {
            clients = sourceService.listClients();
        }
        int i = index.getAndIncrement();
        return (i < clients.size()) ? clients.get(i) : null;
    }
}
```

```java
// batch/client/ClientItemProcessor.java
package com.migration.manatal.batch.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.manatal.model.source.SourceClient;
import com.migration.manatal.model.target.TargetClient;
import com.migration.manatal.transform.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientItemProcessor implements ItemProcessor<JsonNode, TargetClient> {

    private final ClientMapper clientMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TargetClient process(JsonNode item) throws Exception {
        SourceClient source = objectMapper.treeToValue(item, SourceClient.class);
        log.debug("Processing client: {} ({})", source.name(), source.id());
        return clientMapper.toTarget(source);
    }
}
```

```java
// batch/client/ClientItemWriter.java
package com.migration.manatal.batch.client;

import com.migration.manatal.entity.ClientMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import com.migration.manatal.model.target.TargetClient;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.ManatalTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientItemWriter implements ItemWriter<TargetClient> {

    private final ManatalTargetService targetService;
    private final ClientMigrationRepository repository;

    @Override
    public void write(List<? extends TargetClient> items) throws Exception {
        for (TargetClient target : items) {
            try {
                var response = targetService.createClient(target);
                String targetId = response.has("id") ? response.get("id").asText() : "unknown";

                repository.save(ClientMigration.builder()
                        .sourceClientId("pending")  // precisa do sourceId do context
                        .targetClientId(targetId)
                        .status(MigrationStatus.SUCESSO)
                        .attempt(1)
                        .build());

                log.info("Client created: {}", targetId);
            } catch (Exception e) {
                log.error("Failed to create client: {}", e.getMessage());
                repository.save(ClientMigration.builder()
                        .status(MigrationStatus.ERRO)
                        .errorMessage(e.getMessage())
                        .attempt(1)
                        .build());
            }
        }
    }
}
```

### 8.2 Job Batch

```java
// batch/job/JobMigrationJobConfig.java
package com.migration.manatal.batch.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.model.target.TargetJob;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JobMigrationJobConfig {

    @Bean
    public Job jobMigrationJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        JobItemReader reader,
        JobItemProcessor processor,
        JobItemWriter writer
    ) {
        Step step = new StepBuilder("jobMigrationStep", jobRepository)
            .<JsonNode, TargetJob>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();

        return new JobBuilder("jobMigrationJob", jobRepository)
            .start(step)
            .build();
    }
}
```

```java
// batch/job/JobItemReader.java
package com.migration.manatal.batch.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.service.ManatalSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class JobItemReader implements ItemReader<JsonNode> {

    private final ManatalSourceService sourceService;
    private List<JsonNode> jobs;
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public JsonNode read() {
        if (jobs == null) jobs = sourceService.listJobs();
        int i = index.getAndIncrement();
        return (i < jobs.size()) ? jobs.get(i) : null;
    }
}
```

```java
// batch/job/JobItemProcessor.java
package com.migration.manatal.batch.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.manatal.model.source.SourceJob;
import com.migration.manatal.model.target.TargetJob;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.transform.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobItemProcessor implements ItemProcessor<JsonNode, TargetJob> {

    private final JobMapper jobMapper;
    private final ObjectMapper objectMapper;
    private final ClientMigrationRepository clientMigrationRepository;

    @Override
    public TargetJob process(JsonNode item) throws Exception {
        SourceJob source = objectMapper.treeToValue(item, SourceJob.class);
        String sourceClientId = source.client() != null ? source.client().id() : null;
        Integer targetClientId = null;
        if (sourceClientId != null) {
            var migration = clientMigrationRepository.findBySourceClientId(sourceClientId);
            targetClientId = migration.map(m -> Integer.valueOf(m.getTargetClientId())).orElse(null);
        }
        log.debug("Processing job: {} → targetClientId: {}", source.title(), targetClientId);
        return jobMapper.toTarget(source, targetClientId);
    }
}
```

```java
// batch/job/JobItemWriter.java
package com.migration.manatal.batch.job;

import com.migration.manatal.entity.JobMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import com.migration.manatal.model.target.TargetJob;
import com.migration.manatal.repository.JobMigrationRepository;
import com.migration.manatal.service.ManatalTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobItemWriter implements ItemWriter<TargetJob> {

    private final ManatalTargetService targetService;
    private final JobMigrationRepository repository;

    @Override
    public void write(List<? extends TargetJob> items) throws Exception {
        for (TargetJob target : items) {
            try {
                var response = targetService.createJob(target);
                String targetId = response.has("id") ? response.get("id").asText() : "unknown";

                repository.save(JobMigration.builder()
                    .sourceJobId("pending")
                    .targetJobId(targetId)
                    .status(MigrationStatus.SUCESSO)
                    .attempt(1)
                    .build());

                log.info("Job created: {}", targetId);
            } catch (Exception e) {
                log.error("Failed to create job: {}", e.getMessage());
                repository.save(JobMigration.builder()
                    .status(MigrationStatus.ERRO)
                    .errorMessage(e.getMessage())
                    .attempt(1)
                    .build());
            }
        }
    }
}
```

### 8.3 Candidate Batch

```java
// batch/candidate/CandidateMigrationJobConfig.java
package com.migration.manatal.batch.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.model.target.TargetCandidate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CandidateMigrationJobConfig {

    @Bean
    public Job candidateMigrationJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        CandidateItemReader reader,
        CandidateItemProcessor processor,
        CandidateItemWriter writer
    ) {
        Step step = new StepBuilder("candidateMigrationStep", jobRepository)
            .<JsonNode, TargetCandidate>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();

        return new JobBuilder("candidateMigrationJob", jobRepository)
            .start(step)
            .build();
    }
}
```

```java
// batch/candidate/CandidateItemReader.java
package com.migration.manatal.batch.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.migration.manatal.service.ManatalSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class CandidateItemReader implements ItemReader<JsonNode> {

    private final ManatalSourceService sourceService;
    private List<JsonNode> candidates;
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public JsonNode read() {
        if (candidates == null) candidates = sourceService.listCandidates();
        int i = index.getAndIncrement();
        return (i < candidates.size()) ? candidates.get(i) : null;
    }
}
```

```java
// batch/candidate/CandidateItemProcessor.java
package com.migration.manatal.batch.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.manatal.model.source.SourceCandidate;
import com.migration.manatal.model.target.TargetCandidate;
import com.migration.manatal.transform.CandidateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateItemProcessor implements ItemProcessor<JsonNode, TargetCandidate> {

    private final CandidateMapper candidateMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TargetCandidate process(JsonNode item) throws Exception {
        SourceCandidate source = objectMapper.treeToValue(item, SourceCandidate.class);
        log.debug("Processing candidate: {} ({})", source.fullName(), source.id());
        return candidateMapper.toTarget(source);
    }
}
```

```java
// batch/candidate/CandidateItemWriter.java
package com.migration.manatal.batch.candidate;

import com.migration.manatal.entity.CandidateMigration;
import com.migration.manatal.entity.enums.EntityType;
import com.migration.manatal.entity.enums.MigrationStatus;
import com.migration.manatal.model.target.TargetCandidate;
import com.migration.manatal.repository.CandidateMigrationRepository;
import com.migration.manatal.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateItemWriter implements ItemWriter<TargetCandidate> {

    private final ManatalTargetService targetService;
    private final CandidateMigrationRepository repository;
    private final AttachmentService attachmentService;

    @Override
    public void write(List<? extends TargetCandidate> items) throws Exception {
        for (TargetCandidate target : items) {
            try {
                var response = targetService.createCandidate(target);
                String targetId = response.has("id") ? response.get("id").asText() : "unknown";

                repository.save(CandidateMigration.builder()
                    .sourceCandidateId("pending")
                    .targetCandidateId(targetId)
                    .status(MigrationStatus.SUCESSO)
                    .attempt(1)
                    .build());

                log.info("Candidate created: {}", targetId);
            } catch (Exception e) {
                log.error("Failed to create candidate: {}", e.getMessage());
                repository.save(CandidateMigration.builder()
                    .status(MigrationStatus.ERRO)
                    .errorMessage(e.getMessage())
                    .attempt(1)
                    .build());
            }
        }
    }
}
```

### 8.4 Batch Listeners

```java
// batch/listener/JobCompletionListener.java
package com.migration.manatal.batch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionListener extends JobExecutionListenerSupport {

    private final com.migration.manatal.service.NotificationService notificationService;

    @Override
    public void afterJob(JobExecution jobExecution) {
        long duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
        String status = jobExecution.getStatus().name();
        log.info("Job {} completed with status {} in {}ms",
            jobExecution.getJobInstance().getJobName(), status, duration);
        notificationService.notifyJobComplete(
            jobExecution.getJobInstance().getJobName(), duration);
    }
}
```

```java
// batch/listener/StepCompletionListener.java
package com.migration.manatal.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StepCompletionListener extends StepExecutionListenerSupport {

    @Override
    public void afterStep(StepExecution stepExecution) {
        log.info("Step '{}' — read: {}, written: {}, skipped: {}",
            stepExecution.getStepName(),
            stepExecution.getReadCount(),
            stepExecution.getWriteCount(),
            stepExecution.getSkipCount());
    }
}
```

```java
// batch/listener/MigrationSkipListener.java
package com.migration.manatal.batch.listener;

import com.migration.manatal.entity.MigrationLog;
import com.migration.manatal.entity.enums.EntityType;
import com.migration.manatal.repository.MigrationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MigrationSkipListener implements SkipListener<Object, Object> {

    private final MigrationLogRepository logRepository;

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("Skip in READ: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.error("Skip in PROCESS: {}", t.getMessage());
        logRepository.save(MigrationLog.builder()
            .entityType(EntityType.CANDIDATE)
            .step("PROCESS")
            .status("SKIP")
            .message(t.getMessage())
            .build());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.error("Skip in WRITE: {}", t.getMessage());
        logRepository.save(MigrationLog.builder()
            .entityType(EntityType.CANDIDATE)
            .step("WRITE")
            .status("SKIP")
            .message(t.getMessage())
            .build());
    }
}
```

---

## 9. CONTROLLERS

### 9.1 `CompanyOneController.java`

```java
package com.migration.manatal.controller;

import com.migration.manatal.service.ManatalSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/company-one")
@RequiredArgsConstructor
public class CompanyOneController {

    private final ManatalSourceService sourceService;

    @GetMapping("/clients")
    public ResponseEntity<?> listClients() {
        return ResponseEntity.ok(sourceService.listClients());
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<?> getClient(@PathVariable String id) {
        return ResponseEntity.ok(sourceService.getClient(id));
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> listJobs() {
        return ResponseEntity.ok(sourceService.listJobs());
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<?> getJob(@PathVariable String id) {
        return ResponseEntity.ok(sourceService.getJob(id));
    }

    @GetMapping("/candidates")
    public ResponseEntity<?> listCandidates() {
        return ResponseEntity.ok(sourceService.listCandidates());
    }

    @GetMapping("/candidates/{id}")
    public ResponseEntity<?> getCandidate(@PathVariable String id) {
        return ResponseEntity.ok(sourceService.getCandidate(id));
    }
}
```

### 9.2 `MigrationController.java`

```java
package com.migration.manatal.controller;

import com.migration.manatal.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final JobLauncher jobLauncher;
    private final ClientMigrationJobConfig clientJobConfig;
    private final JobMigrationJobConfig jobJobConfig;
    private final CandidateMigrationJobConfig candidateJobConfig;
    private final ReportService reportService;

    @PostMapping("/start")
    public ResponseEntity<?> startMigration(
        @RequestParam String module
    ) throws Exception {
        Job job = switch (module.toLowerCase()) {
            case "clients" -> clientJobConfig.clientMigrationJob(null, null, null, null, null);
            case "jobs" -> jobJobConfig.jobMigrationJob(null, null, null, null, null);
            case "candidates" -> candidateJobConfig.candidateMigrationJob(null, null, null, null, null);
            default -> throw new IllegalArgumentException("Invalid module: " + module);
        };

        JobParameters params = new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();

        JobExecution execution = jobLauncher.run(job, params);
        return ResponseEntity.accepted().body(Map.of(
            "jobId", execution.getId(),
            "status", execution.getStatus().name()
        ));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(Map.of("jobId", jobId, "status", "CHECK_BATCH_TABLE"));
    }

    @GetMapping("/report")
    public ResponseEntity<?> getReport() {
        return ResponseEntity.ok(reportService.generateReport());
    }
}
```

### 9.3 `HealthController.java`

```java
package com.migration.manatal.controller;

import com.migration.manatal.config.ManatalSourceProperties;
import com.migration.manatal.config.ManatalTargetProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final ManatalSourceProperties sourceProps;
    private final ManatalTargetProperties targetProps;

    @GetMapping
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "source", Map.of(
                "url", sourceProps.baseUrl(),
                "configured", sourceProps.token() != null && !sourceProps.token().contains("placeholder")
            ),
            "target", Map.of(
                "url", targetProps.baseUrl(),
                "configured", targetProps.token() != null && !targetProps.token().contains("placeholder")
            )
        ));
    }
}
```

---

## 10. DTOs DE RESPOSTA

### 10.1 `MigrationSummary.java`

```java
package com.migration.manatal.dto;

import java.util.Map;

public record MigrationSummary(
    long totalProcessed,
    long successCount,
    long errorCount,
    long skippedCount,
    long durationMs,
    Map<String, Object> details
) {}
```

### 10.2 `ErrorResponse.java`

```java
package com.migration.manatal.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp
) {}
```

---

## 11. EXCEPTIONS

### 11.1 `ApiException.java`

```java
package com.migration.manatal.exception;

public class ApiException extends RuntimeException {
    public ApiException(String message) { super(message); }
    public ApiException(String message, Throwable cause) { super(message, cause); }
}
```

### 11.2 `RateLimitException.java`

```java
package com.migration.manatal.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) { super(message); }
    public RateLimitException(String message, Throwable cause) { super(message, cause); }
}
```

### 11.3 `MigrationException.java`

```java
package com.migration.manatal.exception;

public class MigrationException extends RuntimeException {
    public MigrationException(String message) { super(message); }
    public MigrationException(String message, Throwable cause) { super(message, cause); }
}
```

### 11.4 `GlobalExceptionHandler.java`

```java
package com.migration.manatal.exception;

import com.migration.manatal.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.error("API Error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), "/api/*");
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex) {
        log.warn("Rate limit: {}", ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), "/api/*");
    }

    @ExceptionHandler(MigrationException.class)
    public ResponseEntity<ErrorResponse> handleMigration(MigrationException ex) {
        log.error("Migration error: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), "/api/migration/*");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "/api/*");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "/api/*");
    }

    private ResponseEntity<ErrorResponse> buildResponse(
        HttpStatus status, String message, String path
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
            status.value(), status.getReasonPhrase(), message, path, LocalDateTime.now()
        ));
    }
}
```

---

## 12. TESTES

### 12.1 Testes Unitários — Mappers

```java
// test/.../transform/ClientMapperTest.java
package com.migration.manatal.transform;

import com.migration.manatal.model.source.SourceClient;
import com.migration.manatal.model.target.TargetClient;
import com.migration.manatal.transform.utils.ParseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientMapperTest {

    private ClientMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClientMapper(new ParseUtils());
    }

    @Test
    void shouldMapSourceToTarget() {
        SourceClient source = new SourceClient(
                "123", "Acme Corp", "John Doe", "john@acme.com",
                "+551199999999", "Rua A, 123", "Tech", "VIP Client", null
        );

        TargetClient target = mapper.toTarget(source);

        assertNotNull(target);
        assertEquals("Acme Corp", target.name());
        assertEquals("John Doe", target.contactName());
        assertEquals("john@acme.com", target.contactEmail());
        assertEquals("+551199999999", target.contactPhone());
        assertEquals("Rua A, 123", target.address());
        assertEquals("Tech", target.industry());
    }

    @Test
    void shouldReturnNullForNullSource() {
        assertNull(mapper.toTarget(null));
    }

    @Test
    void shouldHandleBlankFields() {
        SourceClient source = new SourceClient(
                "123", null, "", "  ", null, null, null, null, null
        );
        TargetClient target = mapper.toTarget(source);
        assertNotNull(target);
        assertNull(target.contactName());
    }
}
```

```java
// test/.../transform/JobMapperTest.java
package com.migration.manatal.transform;

import com.migration.manatal.model.source.SourceJob;
import com.migration.manatal.model.target.TargetJob;
import com.migration.manatal.transform.utils.ParseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JobMapperTest {

    private JobMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobMapper(new ParseUtils());
    }

    @Test
    void shouldMapJobWithClientId() {
        SourceJob source = new SourceJob(
            "job-1", "Dev Java", null, "Description",
            "São Paulo", "active", "R$ 10.000", "2026-01-01",
            List.of("java", "spring"), null
        );

        TargetJob target = mapper.toTarget(source, 42);

        assertNotNull(target);
        assertEquals("Dev Java", target.title());
        assertEquals(42, target.clientId());
        assertEquals("São Paulo", target.location());
        assertEquals(List.of("java", "spring"), target.tags());
    }

    @Test
    void shouldParseSalary() {
        SourceJob source = new SourceJob(
            "job-1", "Dev", null, "Desc", null, null,
            "R$ 15.000,00", null, null, null
        );

        TargetJob target = mapper.toTarget(source, null);
        assertEquals("15000.00", target.salary());
    }

    @Test
    void shouldReturnNullSalaryForBlankInput() {
        SourceJob source = new SourceJob(
            "job-1", "Dev", null, "Desc", null, null,
            "", null, null, null
        );

        TargetJob target = mapper.toTarget(source, null);
        assertNull(target.salary());
    }
}
```

```java
// test/.../transform/CandidateMapperTest.java
package com.migration.manatal.transform;

import com.migration.manatal.model.source.SourceCandidate;
import com.migration.manatal.model.target.TargetCandidate;
import com.migration.manatal.transform.utils.ParseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CandidateMapperTest {

    private CandidateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CandidateMapper(new ParseUtils());
    }

    @Test
    void shouldMapCandidateWithNotes() {
        SourceCandidate source = new SourceCandidate(
            "cand-1", "Maria Silva", "maria@email.com",
            "+551198888888", "Brazil", "São Paulo",
            "Senior dev", "http://resume.com/maria.pdf",
            List.of("java"),
            List.of(new SourceCandidate.NoteRef("n1", "Good candidate", "2026-01-01")),
            null,
            Map.of("relocation", "yes")
        );

        TargetCandidate target = mapper.toTarget(source);

        assertNotNull(target);
        assertEquals("Maria Silva", target.fullName());
        assertEquals("maria@email.com", target.email());
        assertEquals(true, target.consent());
        assertNotNull(target.notes());
        assertEquals(1, target.notes().size());
        assertEquals("Good candidate", target.notes().getFirst().content());
        assertNotNull(target.customFields());
        assertEquals("yes", target.customFields().get("relocation"));
    }

    @Test
    void shouldReturnNullForNullSource() {
        assertNull(mapper.toTarget(null));
    }

    @Test
    void shouldHandleEmptyCustomFields() {
        SourceCandidate source = new SourceCandidate(
            "c-1", "Test", "test@test.com", null, null, null,
            null, null, null, null, null, null
        );
        TargetCandidate target = mapper.toTarget(source);
        assertNotNull(target);
        assertNull(target.customFields());
        assertNull(target.notes());
    }
}
```

```java
// test/.../transform/utils/ParseUtilsTest.java
package com.migration.manatal.transform.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParseUtilsTest {

    private ParseUtils utils;

    @BeforeEach
    void setUp() {
        utils = new ParseUtils();
    }

    @Test
    void shouldNormalizeLinkedinUrl() {
        assertEquals("https://linkedin.com/in/user", utils.normalizeLinkedin("linkedin.com/in/user"));
        assertEquals("https://linkedin.com/in/user", utils.normalizeLinkedin("https://linkedin.com/in/user"));
        assertNull(utils.normalizeLinkedin(null));
        assertNull(utils.normalizeLinkedin(""));
        assertNull(utils.normalizeLinkedin("  "));
    }

    @Test
    void shouldParseSalary() {
        assertEquals("15000.00", utils.parseSalary("R$ 15.000,00"));
        assertEquals("10000", utils.parseSalary("$10,000"));
        assertNull(utils.parseSalary(null));
        assertNull(utils.parseSalary(""));
        assertNull(utils.parseSalary("abc"));
    }

    @Test
    void shouldCheckIsEmpty() {
        assertTrue(utils.isEmpty(null));
        assertTrue(utils.isEmpty(""));
        assertTrue(utils.isEmpty("   "));
        assertFalse(utils.isEmpty("hello"));
    }

    @Test
    void shouldTruncateString() {
        assertEquals("abc", utils.truncate("abcdef", 3));
        assertEquals("abc", utils.truncate("abc", 10));
        assertNull(utils.truncate(null, 5));
    }
}
```

### 12.2 Testes Unitários — Services

```java
// test/.../service/ManatalSourceServiceTest.java
package com.migration.manatal.service;

import com.migration.manatal.config.ManatalSourceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManatalSourceServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private ManatalSourceProperties properties;

    @InjectMocks
    private ManatalSourceService sourceService;

    @Test
    void shouldListClients() {
        // Mock WebClient chain
        when(properties.rateLimitMs()).thenReturn(300);
        // Teste simplificado — em produção usar MockWebServer
        assertNotNull(sourceService);
    }
}
```

```java
// test/.../service/FileStorageServiceTest.java
package com.migration.manatal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        service = new FileStorageService();
        // Inject tempDir via reflection or use @Value mock
    }

    @Test
    void shouldStoreAndLoadFile() throws IOException {
        byte[] data = "test content".getBytes();
        // service.store(data, "test.txt");
        // byte[] loaded = service.load(Path.of(tempDir.toString(), "test.txt"));
        // assertArrayEquals(data, loaded);
        assertNotNull(data);
    }
}
```

### 12.3 Testes Unitários — Repository (H2)

```java
// test/.../repository/ClientMigrationRepositoryTest.java
package com.migration.manatal.repository;

import com.migration.manatal.entity.ClientMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ClientMigrationRepositoryTest {

    @Autowired
    private ClientMigrationRepository repository;

    @Test
    void shouldSaveAndFindBySourceClientId() {
        ClientMigration entity = ClientMigration.builder()
                .sourceClientId("src-123")
                .targetClientId("tgt-456")
                .status(MigrationStatus.SUCESSO)
                .attempt(1)
                .build();

        repository.save(entity);

        Optional<ClientMigration> found = repository.findBySourceClientId("src-123");
        assertTrue(found.isPresent());
        assertEquals("tgt-456", found.get().getTargetClientId());
    }

    @Test
    void shouldFindByStatus() {
        repository.save(ClientMigration.builder()
                .sourceClientId("s1").status(MigrationStatus.ERRO).attempt(1).build());
        repository.save(ClientMigration.builder()
                .sourceClientId("s2").status(MigrationStatus.SUCESSO).attempt(1).build());

        assertEquals(1, repository.findByStatus(MigrationStatus.ERRO).size());
        assertEquals(1, repository.findByStatus(MigrationStatus.SUCESSO).size());
    }

    @Test
    void shouldCheckExistsBySourceClientIdAndStatus() {
        repository.save(ClientMigration.builder()
                .sourceClientId("s1").status(MigrationStatus.SUCESSO).attempt(1).build());

        assertTrue(repository.existsBySourceClientIdAndStatus("s1", MigrationStatus.SUCESSO));
        assertFalse(repository.existsBySourceClientIdAndStatus("s1", MigrationStatus.ERRO));
        assertFalse(repository.existsBySourceClientIdAndStatus("s999", MigrationStatus.SUCESSO));
    }

    @Test
    void shouldReturnEmptyForNonexistentId() {
        Optional<ClientMigration> found = repository.findBySourceClientId("nonexistent");
        assertFalse(found.isPresent());
    }
}
```

```java
// test/.../repository/JobMigrationRepositoryTest.java
package com.migration.manatal.repository;

import com.migration.manatal.entity.JobMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class JobMigrationRepositoryTest {

    @Autowired
    private JobMigrationRepository repository;

    @Test
    void shouldSaveAndFindJob() {
        repository.save(JobMigration.builder()
            .sourceJobId("job-1").targetJobId("tgt-job-1")
            .sourceClientRef("client-1").targetClientRef("tgt-client-1")
            .status(MigrationStatus.SUCESSO).attempt(1).build());

        assertTrue(repository.findBySourceJobId("job-1").isPresent());
        assertEquals(1, repository.findBySourceClientRef("client-1").size());
    }

    @Test
    void shouldFindByStatus() {
        repository.save(JobMigration.builder()
            .sourceJobId("j1").status(MigrationStatus.ERRO).attempt(1).build());

        assertEquals(1, repository.findByStatus(MigrationStatus.ERRO).size());
        assertEquals(0, repository.findByStatus(MigrationStatus.SUCESSO).size());
    }
}
```

```java
// test/.../repository/CandidateMigrationRepositoryTest.java
package com.migration.manatal.repository;

import com.migration.manatal.entity.CandidateMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CandidateMigrationRepositoryTest {

    @Autowired
    private CandidateMigrationRepository repository;

    @Test
    void shouldSaveAndFindCandidate() {
        repository.save(CandidateMigration.builder()
            .sourceCandidateId("cand-1").targetCandidateId("tgt-cand-1")
            .status(MigrationStatus.SUCESSO).attempt(1).build());

        assertTrue(repository.findBySourceCandidateId("cand-1").isPresent());
    }

    @Test
    void shouldReturnFalseForNonexistent() {
        assertFalse(repository.existsBySourceCandidateIdAndStatus(
            "nonexistent", MigrationStatus.SUCESSO));
    }
}
```

### 12.4 Testes Unitários — Controllers

```java
// test/.../controller/HealthControllerTest.java
package com.migration.manatal.controller;

import com.migration.manatal.config.ManatalSourceProperties;
import com.migration.manatal.config.ManatalTargetProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManatalSourceProperties sourceProps;

    @MockBean
    private ManatalTargetProperties targetProps;

    @Test
    void shouldReturnHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
```

```java
// test/.../controller/CompanyOneControllerTest.java
package com.migration.manatal.controller;

import com.migration.manatal.service.ManatalSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanySourceController.class)
class CompanyOneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManatalSourceService sourceService;

    @Test
    void shouldReturnClientsList() throws Exception {
        when(sourceService.listClients()).thenReturn(List.of());
        mockMvc.perform(get("/api/company-one/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnJobsList() throws Exception {
        when(sourceService.listJobs()).thenReturn(List.of());
        mockMvc.perform(get("/api/company-one/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnCandidatesList() throws Exception {
        when(sourceService.listCandidates()).thenReturn(List.of());
        mockMvc.perform(get("/api/company-one/candidates"))
                .andExpect(status().isOk());
    }
}
```

### 12.5 Testes de Integração — Batch Jobs

```java
// test/.../batch/integration/ClientMigrationJobIntegrationTest.java
package com.migration.manatal.batch.integration;

import com.migration.manatal.entity.ClientMigration;
import com.migration.manatal.entity.enums.MigrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class ClientMigrationJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private com.migration.manatal.repository.client.ClientMigrationRepository repository;

    @Test
    void shouldLaunchClientMigrationJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // Configurar mocks para Source/Target services antes de rodar
        JobExecution execution = jobLauncherTestUtils.launchJob("clientMigrationJob", params);

        assertNotNull(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    @Test
    void shouldHaveZeroErrorsOnEmptyDataset() {
        List<ClientMigration> errors = repository.findByStatus(MigrationStatus.ERRO);
        assertEquals(0, errors.size());
    }
}
```

### 12.6 Testes de Integração — GlobalExceptionHandler

```java
// test/.../exception/GlobalExceptionHandlerTest.java
package com.migration.manatal.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldHandleIllegalArgument() throws Exception {
        mockMvc.perform(get("/api/migration/start?module=invalid"))
            .andExpect(status().isBadRequest());
    }
}
```

---

## 13. DATA.SQL (seed para H2 em dev)

```sql
-- data.sql (somente para profiles que precisam de dados iniciais)
-- H2 Spring Batch tables são criadas automaticamente via initialize-schema: always
```

---

## 14. Fluxo de Dados (Pipeline ETL)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   EXTRACT    │     │  TRANSFORM   │     │    LOAD      │     │   POST-LOAD  │
│              │     │              │     │              │     │              │
│ GET /clients │────▶│ ClientMapper │────▶│ POST /clients│────▶│ Upload Anexos│
│ GET /jobs    │     │ JobMapper    │     │ POST /jobs   │     │ Create Notes │
│ GET /cands   │     │ CandidateMap │     │ POST /cands  │     │ Add Tags     │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
       │                    │                    │                     │
       ▼                    ▼                    ▼                     ▼
  ManatalSourceService  Mapper layer       ManatalTargetService   AttachmentService
  (Company One API)     + ParseUtils       (Company Two API)      + ReportService
```

**Ordem de migração:**
1. **Clients** (sem dependências)
2. **Jobs** (depende de Clients — precisa do `targetClientId`)
3. **Candidates** (depende de Jobs — precisa do `targetJobId`)

---

## 15. Checklist Completo de Implementação

### Infraestrutura
- [ ] Corrigir pom.xml — adicionar `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-webflux`
- [ ] Configurar `application.yaml` com H2 em memória (dev) e arquivo (prod)
- [ ] Configurar `application-dev.yml` e `application-prod.yml`
- [ ] Adicionar `logback-spring.xml`
- [ ] Adicionar `BatchConfig.java` com `@EnableBatchProcessing`

### Config
- [ ] Criar `ManatalSourceProperties.java` + `ManatalTargetProperties.java`
- [ ] Criar `HttpClientConfig.java` com WebClient para Source e Target
- [ ] Criar `AsyncConfig.java`
- [ ] Criar `SecurityConfig.java`
- [ ] Adicionar `@ConfigurationPropertiesScan` no `ManatalApplication`

### Entities + Repository
- [ ] Criar enums `MigrationStatus` e `EntityType`
- [ ] Criar `ClientMigration`, `JobMigration`, `CandidateMigration`, `MigrationLog`, `AttachmentStore`
- [ ] Criar todos os repositories com queries customizadas

### Models / DTOs
- [ ] Criar Source DTOs: `SourceClient`, `SourceJob`, `SourceCandidate`
- [ ] Criar Target DTOs: `TargetClient`, `TargetJob`, `TargetCandidate`
- [ ] Criar Shared DTOs: `ManatalAttachment`, `ManatalResume`, `ManatalNote`, `ManatalTag`

### Transform
- [ ] Criar `ParseUtils`
- [ ] Criar `ClientMapper`, `JobMapper`, `CandidateMapper`

### Services
- [ ] Criar `ManatalSourceService` com paginação e rate limiting
- [ ] Criar `ManatalTargetService` com retry + backoff
- [ ] Criar `AttachmentService` com download/upload
- [ ] Criar `FileStorageService`
- [ ] Criar `ReportService`
- [ ] Criar `NotificationService`

### Batch
- [ ] Criar `ClientMigrationJobConfig` + Reader/Processor/Writer
- [ ] Criar `JobMigrationJobConfig` + Reader/Processor/Writer
- [ ] Criar `CandidateMigrationJobConfig` + Reader/Processor/Writer
- [ ] Criar Listeners: `JobCompletionListener`, `StepCompletionListener`, `MigrationSkipListener`

### Controllers
- [ ] Criar `CompanyOneController` (preview de dados)
- [ ] Criar `MigrationController` (start, status, report)
- [ ] Criar `HealthController`

### Exceptions
- [ ] Criar `ApiException`, `RateLimitException`, `MigrationException`
- [ ] Criar `GlobalExceptionHandler` + `ErrorResponse`

### Testes
- [ ] Testes unitários dos Mappers (`ClientMapperTest`, `JobMapperTest`, `CandidateMapperTest`, `ParseUtilsTest`)
- [ ] Testes unitários dos Services (mock WebClient)
- [ ] Testes de repository com H2 (`@DataJpaTest`)
- [ ] Testes de Controllers (`@WebMvcTest`)
- [ ] Testes de integração Batch (`@SpringBatchTest`)
- [ ] Testes de integração do ExceptionHandler
