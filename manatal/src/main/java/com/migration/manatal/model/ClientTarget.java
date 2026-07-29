package com.migration.manatal.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientTarget {

    private String clientName;
    private String clientWebsite;
    private Map<String, Object> clientIndustry;
    private String clientLocation;
    private String clientDescription;
    private Integer clientOwner;
    private String teamMembers;
    private String stage;


}
