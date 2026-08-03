package com.migration.manatal.repository.job;


import com.migration.manatal.entity.job.JobMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobMigrationRepository extends JpaRepository<JobMigration, Long> {
    Optional<JobMigration> findBySourceJobId(String sourceJobId);
    List<JobMigration> findByStatus(String status);
}
