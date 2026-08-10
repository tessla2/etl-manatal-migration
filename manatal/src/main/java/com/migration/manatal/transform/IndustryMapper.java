package com.migration.manatal.transform;

import com.migration.manatal.service.job.ManatalTargetJobService;
import com.migration.manatal.service.job.ManatalTargetJobService.IndustryTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndustryMapper {

    private final ManatalTargetJobService targetJobService;

    private volatile Map<String, Integer> nameToId;

    public Integer resolve(String sourceIndustryName) {
        if (sourceIndustryName == null || sourceIndustryName.isBlank()) {
            return null;
        }
        Map<String, Integer> map = load();
        Integer targetId = map.get(sourceIndustryName);
        if (targetId == null) {
            log.warn("Industry '{}' not found in target; leaving industry empty in target", sourceIndustryName);
            return null;
        }
        return targetId;
    }

    private Map<String, Integer> load() {
        Map<String, Integer> local = nameToId;
        if (local == null) {
            synchronized (this) {
                local = nameToId;
                if (local == null) {
                    local = fetch();
                    nameToId = local;
                }
            }
        }
        return local;
    }

    private Map<String, Integer> fetch() {
        List<IndustryTarget> industries = targetJobService.listIndustries();
        Map<String, Integer> map = new HashMap<>();
        for (IndustryTarget industry : industries) {
            if (industry.name() != null) {
                map.put(industry.name(), industry.id());
            }
        }
        log.info("Target industries loaded: {} mapped by name", map.size());
        return map;
    }
}
