package com.migration.manatal.sandbox;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.model.job.JobSource;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.service.job.ManatalTargetJobService;
import com.migration.manatal.transform.OwnerMapper;
import com.migration.manatal.transform.JobMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EnabledIfEnvironmentVariable(named = "MANATAL_TARGET_TOKEN", matches = ".+")
class ManatalPostSandboxJobTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");

    private static final String SAMPLE_SOURCE_JOB = """
            {
              "position_name": "Mock Software Developer",
              "description": "Syffer is an all-inclusive consulting company focused on talent, tech and innovation. We exist to elevate companies and humans all around the world, making change, from the inside to the outside.
            
                              We believe that technology + human kindness positively impacts every community around the world. Our approach is simple, we see a world without borders, and believe in equal opportunities. We are guided by our core principles of spreading positivity, good energy and promote equality and care for others.
            
                              Our hiring process is unique! People are selected by their value, education, talent and personality. We dont present ethnicity, religion, national origin, age, gender, sexual orientation or identity.
            
                              Its time to burst the bubble, and we will do it together!
            
                              What You'll do:
            
                              - Develop and maintain Java-based microservices supporting digital payment solutions;
            
                              - Design, implement, and consume REST and gRPC APIs;
            
                              - Build event-driven applications using Apache Kafka;
            
                              - Migrate applications from on-premises infrastructure to public cloud environments;
            
                              - Optimize application performance, scalability, resilience, and availability;
            
                              - Implement and maintain CI/CD pipelines for automated deployments;
            
                              - Monitor production environments using Prometheus and Grafana;
            
                              - Develop and maintain automated test suites;
            
                              - Collaborate with cross-functional teams to deliver secure and reliable payment services;
            
                              - Hybrid work model;\s
            
            
                              Who You Are:
            
                              - Strong experience with Java and Spring Boot;
            
                              - Proven experience developing Microservices architectures;
            
                              - Experience designing and implementing REST and/or gRPC APIs;
            
                              - Hands-on experience with Apache Kafka;
            
                              - Experience deploying applications on Pivotal Cloud Foundry (PCF) or similar cloud platforms;
            
                              - Experience with CI/CD tools and automated deployment pipelines;
            
                              - Hands-on experience with Prometheus and Grafana for monitoring and observability;
            
                              - Experience with automated testing frameworks (e.g., JUnit, Mockito, Testcontainers);
            
                              - Knowledge of distributed systems design, including scalability, fault tolerance, and resilience;
            
                              - Experience developing and deploying applications in cloud environments (private and/or public);
            
                              - Familiarity with containerized application deployment and cloud-native architectures;
            
                              - Fluent in portuguese and english;\s
            
                              ﻿﻿
            
                              What you'll get:
            
                              - Wage according to candidate's professional experience;
            
                              - Remote Work whenever possible;
            
                              - Delivery of work equipment adjusted to the performance of functions;
            
                              ﻿- Benefits plan;
            
                              - And others.
            
                              Work together with expert teams on projects of large magnitude and intensity, long term together with our clients, all leaders in their industries.
            
                              Are you ready to step into a diverse and inclusive world with us?
            
                              Together we will promote uniquess!",
              "headcount": 1,
              "organization": 3779409, 
              "owner": 123,
              "status": "planning",
              "city": "Lisboa",
              "country": "Portugal",
              "state": "Porto",
              "open_at": "2026-08-01T09:00:00Z",
              "close_at": null,
              "contract_details": "full_time",
              "industry": { "id": 384181, "name": "Accounting / Audit / Tax Services" },
              "custom_fields": {
                "rate": "target rate: 220€-230€/dia",
                "costday": 20,
                "rateday": 1,
                "category": ["IT"],
                "portugus": ["Obrigatório"],
                "inherited": false,
                "workplace": "Hybrid",
                "atualstatus": "Active",
                "contactname": "7728214",
                "grossmargin": 30,
                "ratehistory": "taxa anterior: 150",
                "businessunit": "PT - IT",
                "englishlevel": "Advanced (C1,C2)",
                "startdatejob": "2026-08-10",
                "morethanmonth": true,
                "purchaseorder": "1",
                "activebusiness": true,
                "consultantname": "Consultor Teste",
                "headcountatual": 2,
                "officelocation": ["Lisboa"],
                "technicalskill": ["Java", "Spring"],
                "internalization": "After 12 Months",
                "startdatesyffer": "2026-08-15",
                "invoicepaymentterm": "30 Days",
                "firstjobclosedinclient": false,
                "experiencelevelofficial": ["Middle"],
                "replacepreviousposition": false,
                "jobadditionalinformation": "Detalhe adicional do mock."
              }
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobMapper jobMapper = new JobMapper(new OwnerMapper(new OwnerMappingProperties()));
    private ManatalTargetJobService targetService;

    @BeforeEach
    void setUp() throws Exception {
        targetService = new ManatalTargetJobService(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), objectMapper);

        var baseUrlField = ManatalTargetJobService.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(targetService, BASE_URL);

        var tokenField = ManatalTargetJobService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(targetService, System.getenv("MANATAL_TARGET_TOKEN"));

        var retrySecondsField = ManatalTargetJobService.class.getDeclaredField("rateLimitRetrySeconds");
        retrySecondsField.setAccessible(true);
        retrySecondsField.set(targetService, 1);

        var retryLimitField = ManatalTargetJobService.class.getDeclaredField("retryLimit");
        retryLimitField.setAccessible(true);
        retryLimitField.set(targetService, 3);
    }

    @Test
    void shouldPostMockJobAndNoteToTarget() throws Exception {
        JobSource source = objectMapper.readValue(SAMPLE_SOURCE_JOB, JobSource.class);

        JobSource.JobNote note = new JobSource.JobNote();
        note.setContent("Nota de teste: escrita no dia 31/07 para validar o POST de notas.");
        note.setCreator(1);
        note.setCreatedAt("2026-07-31T10:00:00Z");

        JobTarget target = jobMapper.toTarget(source, List.of(note));
        target.setPositionName("[MIGRATION-TEST] " + target.getPositionName());
        target.setHeadcount(1);

        log.info("=== PAYLOAD SENT TO POST /jobs/ ===");
        log.info(objectMapper.writeValueAsString(target));

        String response = targetService.migrateJob(target);
        log.info("=== RESPONSE POST /jobs/ ===");
        log.info(response);

        long targetJobId = objectMapper.readTree(response).path("id").asLong();
        assertTrue(targetJobId > 0, "Expected a target job id in response: " + response);

        log.info("=== PAYLOAD SENT TO POST /jobs/{}/notes/ ===", targetJobId);
        log.info("{\"info\": \"Nota de validacao pos-criacao.\"}");

        String noteResponse = targetService.createJobNote((int) targetJobId, "Nota de validacao pos-criacao.");
        log.info("=== RESPONSE POST note (job {}) ===", targetJobId);
        log.info(noteResponse);
    }
}
