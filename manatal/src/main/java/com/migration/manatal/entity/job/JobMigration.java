package com.migration.manatal.entity.job;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "job_migration")
public class JobMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "source_job_id")
    private String sourceJobId;
    @Column(name = "source_position_name")
    private String positionName;
    @Column(name = "source_organization_id")
    private String sourceOrganizationId;
    @Column(name = "target_organization_id")
    private Long targetOrganizationId;
    @Column(name = "status")
    private String status;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "tagged_in_source")
    private Boolean taggedInSource;

    @Column(name = "target_job_id")
    private Long targetJobId;

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
