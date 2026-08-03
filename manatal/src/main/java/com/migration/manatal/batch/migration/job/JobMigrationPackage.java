package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.model.job.JobTarget;
import lombok.Data;

@Data
public class JobMigrationPackage {

    private JobMigration entity;
    private JobTarget transformed;
    private Long targetJobId;
    private String errorMessage;

}
