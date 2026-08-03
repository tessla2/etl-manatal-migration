package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.repository.job.JobMigrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class JobMigrationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager; // Spring interface to manage transactions, ex. if 50 itens are processed and 1 fails, the transaction manager will rollback all 50 items to avoid data inconsistency
    private final JobMigrationRepository jobMigrationRepository;
    private final LoadJobsTasklet loadJobsTasklet;
    private final JobMigrationProcessor processor;
    private final JobMigrationWriter writer;


    @Value("${migration.batch.chunk-size}")
    private int chunkSize;

    @Value("${migration.batch.retry-limit}")
    private int retryLimit;

    @Value("${migration.batch.skip-limit}")
    private int skipLimit;


    @Bean
    public Job jobMigrationJob() {
        return new JobBuilder("jobMigrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadJobsStep())
                .next(migrateJobsStep())
                .build();

    }

    @Bean
    public Step loadJobsStep() {
        return new StepBuilder("loadJobsStep", jobRepository)
                .tasklet(loadJobsTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step migrateJobsStep() {
        return new StepBuilder("migrateJobsStep", jobRepository)
                .<JobMigration, JobMigrationPackage>chunk(chunkSize)
                .reader(jobReader())
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
    public ItemReader<JobMigration> jobReader() {
        RepositoryItemReader<JobMigration> reader = new RepositoryItemReader<>(
                jobMigrationRepository, Map.of("id", Sort.Direction.ASC));
        reader.setMethodName("findByStatus");
        reader.setArguments(List.of("PENDENTE"));
        reader.setPageSize(chunkSize);
        return reader;

    }

}
