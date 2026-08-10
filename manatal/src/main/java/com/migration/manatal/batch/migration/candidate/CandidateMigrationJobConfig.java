package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class CandidateMigrationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final LoadCandidatesTasklet loadCandidatesTasklet;
    private final CandidateMigrationProcessor processor;
    private final CandidateMigrationWriter writer;
    private final PendingCandidatesReader pendingCandidatesReader;
    private final MarkCandidatesExportedTasklet markCandidatesExportedTasklet;

    @Value("${migration.batch.chunk-size}")
    private int chunkSize;

    @Value("${migration.batch.retry-limit}")
    private int retryLimit;

    @Value("${migration.batch.skip-limit}")
    private int skipLimit;

    @Bean
    public Job candidateMigrationJob() {
        return new JobBuilder("candidateMigrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadCandidatesStep())
                .next(migrateCandidatesStep())
                .next(markExportedStep())
                .build();
    }

    @Bean
    public Step loadCandidatesStep() {
        return new StepBuilder("loadCandidatesStep", jobRepository)
                .tasklet(loadCandidatesTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step markExportedStep() {
        return new StepBuilder("markExportedStep", jobRepository)
                .tasklet(markCandidatesExportedTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step migrateCandidatesStep() {
        return new StepBuilder("migrateCandidatesStep", jobRepository)
                .<CandidateMigration, CandidateMigrationPackage>chunk(chunkSize)
                .reader(candidateReader())
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .faultTolerant()
                .retryLimit(retryLimit)
                .retry(ApiException.class, RateLimitException.class)
                .skipLimit(skipLimit)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public ItemReader<CandidateMigration> candidateReader() {
        return pendingCandidatesReader;
    }

}
