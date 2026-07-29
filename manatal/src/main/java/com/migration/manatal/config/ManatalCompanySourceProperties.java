package com.migration.manatal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "migration.manatal.company-one")
public record ManatalCompanySourceProperties(
        OAuth oauth,
        String baseUrl
) {
    public record OAuth(
            String tokenUrl
    ) {
    }
}