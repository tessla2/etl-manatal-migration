package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.repository.client.ClientMigrationRepository;
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
public class ClientMigrationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ClientMigrationRepository clientMigrationRepository;
    private final LoadClientsTasklet loadClientsTasklet;
    private final ClientMigrationProcessor processor;
    private final ClientMigrationWriter writer;

    @Value("${migration.batch.chunk-size:1}")
    private int chunkSize;

    @Value("${migration.batch.retry-limit:3}")
    private int retryLimit;

    @Value("${migration.batch.skip-limit:10}")
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
                .retry(ApiException.class)
                .skipLimit(skipLimit)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public ItemReader<ClientMigration> clientReader() {
        RepositoryItemReader<ClientMigration> reader = new RepositoryItemReader<>(
                clientMigrationRepository, Map.of("id", Sort.Direction.ASC));
        reader.setMethodName("findByStatus");
        reader.setArguments(List.of("PENDENTE"));
        reader.setPageSize(chunkSize);
        return reader;
    }
}
