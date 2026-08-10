package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
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
public class ClientMigrationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager; // Spring interface to manage transactions, ex. if 50 itens are processed and 1 fails, the transaction manager will rollback all 50 items to avoid data inconsistency
    private final LoadClientsTasklet loadClientsTasklet;
    private final ClientMigrationProcessor processor;
    private final ClientMigrationWriter writer;
    private final PendingClientsReader pendingClientsReader;

    @Value("${migration.batch.chunk-size}")
    private int chunkSize;

    @Value("${migration.batch.retry-limit}")
    private int retryLimit;

    @Value("${migration.batch.skip-limit}")
    private int skipLimit;

    @Bean
    public Job clientMigrationJob() {
        return new JobBuilder("clientMigrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadClientsStep())
                .next(migrateClientsStep())
                .build();
    }

    @Bean
    public Step loadClientsStep() {
        return new StepBuilder("loadClientsStep", jobRepository)
                .tasklet(loadClientsTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step migrateClientsStep() {
        return new StepBuilder("migrateClientsStep", jobRepository)
                .<ClientMigration, ClientMigrationPackage>chunk(chunkSize)
                .reader(clientReader())
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
    public ItemReader<ClientMigration> clientReader() {
        return pendingClientsReader;
    }
}
