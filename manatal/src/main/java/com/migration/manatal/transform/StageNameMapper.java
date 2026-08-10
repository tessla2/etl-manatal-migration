package com.migration.manatal.transform;

import com.migration.manatal.config.OwnerMappingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StageNameMapper {

    private final OwnerMappingProperties properties;

    public String resolve(String sourceStageName) {
        if (sourceStageName == null || sourceStageName.isBlank()) {
            return null;
        }
        Map<String, String> mapping = properties.getStageMapping();
        String target = mapping.get(sourceStageName);
        if (target == null || target.isBlank()) {
            target = mapping.get(sourceStageName.replace(" ", ""));
        }
        if (target == null || target.isBlank()) {
            log.warn("No target stage mapped for source stage '{}'; using same name", sourceStageName);
            return sourceStageName;
        }
        return target;
    }
}
