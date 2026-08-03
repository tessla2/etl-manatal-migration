package com.migration.manatal.transform;

import com.migration.manatal.config.OwnerMappingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnerMapper {

    private static final int DEFAULT_OWNER_ID = 1193857;

    private final OwnerMappingProperties properties;

    public int resolve(Integer sourceOwnerId) {
        if (sourceOwnerId == null) {
            return DEFAULT_OWNER_ID;
        }
        return properties.getOwnerMapping().getOrDefault(sourceOwnerId, DEFAULT_OWNER_ID);
    }
}
