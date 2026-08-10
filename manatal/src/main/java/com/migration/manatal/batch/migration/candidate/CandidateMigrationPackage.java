package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.model.candidate.CandidateTarget;
import lombok.Data;

@Data
public class CandidateMigrationPackage {

    private CandidateMigration entity;
    private CandidateTarget transformed;
    private Long targetCandidateId;
    private String errorMessage;

}
