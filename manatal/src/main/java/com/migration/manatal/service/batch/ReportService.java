package com.migration.manatal.service.batch;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.repository.job.JobMigrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClientMigrationRepository clientRepository;
    private final JobMigrationRepository jobRepository;
    private final CandidateMigrationRepository candidateRepository;

    public Map<String, Object> generateSummary() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("total", countTotal());
        report.put("sucesso", countStatus("SUCESSO"));
        report.put("erro", countStatus("ERRO"));
        report.put("pendente", countStatus("PENDENTE"));

        Map<String, Map<String, Object>> porTipo = new LinkedHashMap<>();
        porTipo.put("CLIENT", statsFor(clientRepository.countByStatus("SUCESSO"),
                clientRepository.countByStatus("ERRO"), clientRepository.countByStatus("PENDENTE")));
        porTipo.put("JOB", statsFor(jobRepository.countByStatus("SUCESSO"),
                jobRepository.countByStatus("ERRO"), jobRepository.countByStatus("PENDENTE")));
        porTipo.put("CANDIDATE", statsFor(candidateRepository.countByStatus("SUCESSO"),
                candidateRepository.countByStatus("ERRO"), candidateRepository.countByStatus("PENDENTE")));
        report.put("porTipo", porTipo);

        return report;
    }

    public List<Map<String, Object>> listErrors(int limit) {
        List<Map<String, Object>> errors = new ArrayList<>();
        errors.addAll(clientRepository.findByStatus("ERRO").stream()
                .map(e -> entry("CLIENT", e.getId(), e.getSourceOrganizationId(), e.getSourceName(),
                        e.getTargetOrganizationId(), e.getErrorMessage(), e.getUpdatedAt()))
                .toList());
        errors.addAll(jobRepository.findByStatus("ERRO").stream()
                .map(e -> entry("JOB", e.getId(), e.getSourceJobId(), e.getPositionName(),
                        e.getTargetJobId(), e.getErrorMessage(), e.getUpdatedAt()))
                .toList());
        errors.addAll(candidateRepository.findByStatus("ERRO").stream()
                .map(e -> entry("CANDIDATE", e.getId(), e.getSourceCandidateId(), e.getFullName(),
                        e.getTargetCandidateId(), e.getErrorMessage(), e.getUpdatedAt()))
                .toList());
        return errors.stream()
                .sorted(Comparator.comparing((Map<String, Object> e) -> (String) e.get("updatedAt"),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private Map<String, Object> entry(String tipo, Long id, String sourceId, String name, Long targetId,
                                      String errorMessage, java.time.LocalDateTime updatedAt) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("tipo", tipo);
        entry.put("id", id);
        entry.put("sourceId", sourceId);
        entry.put("name", name);
        entry.put("targetId", targetId);
        entry.put("errorMessage", errorMessage);
        entry.put("updatedAt", updatedAt != null ? updatedAt.format(DATE_FORMATTER) : null);
        return entry;
    }

    private Map<String, Object> statsFor(long sucesso, long erro, long pendente) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", sucesso + erro + pendente);
        stats.put("sucesso", sucesso);
        stats.put("erro", erro);
        stats.put("pendente", pendente);
        return stats;
    }

    private long countTotal() {
        return clientRepository.count() + jobRepository.count() + candidateRepository.count();
    }

    private long countStatus(String status) {
        return clientRepository.countByStatus(status)
                + jobRepository.countByStatus(status)
                + candidateRepository.countByStatus(status);
    }
}
