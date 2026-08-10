package com.migration.manatal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "migration.manatal")
public class OwnerMappingProperties {

    private Map<Integer, Integer> ownerMapping = new HashMap<>();

    private Map<Integer, String> creatorNameMapping = new HashMap<>();

    private Map<String, String> stageMapping = new HashMap<>();

}
