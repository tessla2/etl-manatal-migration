package com.migration.manatal.repository.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateMigrationRepository extends JpaRepository<CandidateMigration, Long> {
    Optional<CandidateMigration> findBySourceCandidateId(String sourceCandidateId);
    List<CandidateMigration> findByStatus(String status);
    long countByStatus(String status);
}
