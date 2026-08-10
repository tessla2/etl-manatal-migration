package com.migration.manatal.entity.candidate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "candidate_migration")
public class CandidateMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "source_candidate_id")
    private String sourceCandidateId;
    @Column(name = "source_full_name")
    private String fullName;
    @Column(name = "status")
    private String status;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "target_candidate_id")
    private Long targetCandidateId;
    @Column(name = "source_job_id")
    private String sourceJobId;
    @Column(name = "target_job_id")
    private Long targetJobId;
    @Column(name = "stage_name")
    private String stageName;
    @Column(name = "tagged_in_source")
    private Boolean taggedInSource;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
