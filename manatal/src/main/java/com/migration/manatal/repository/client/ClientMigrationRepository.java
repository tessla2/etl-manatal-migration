package com.migration.manatal.repository.client;


import com.migration.manatal.entity.client.ClientMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientMigrationRepository extends JpaRepository<ClientMigration, Long> {
    Optional<ClientMigration> findBySourceOrganizationId(String sourceOrganizationId);
    List<ClientMigration> findByStatus(String status);
}
