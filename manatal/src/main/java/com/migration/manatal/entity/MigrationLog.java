package com.migration.manatal.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "migration_log")
public class MigrationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long migrationEntityId; // FK to the entity being migrated (e.g., candidate, organization)

    private String entityType; // Client, Job, Candidate, etc.
    private String step;
    private String status; // Success, Error, etc.

    private String message;
    private LocalDateTime createdAt;



}
