package com.migration.manatal.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientSource {

    @JsonProperty("name")
    private String clientName;
    @JsonProperty("website")
    private String clientWebsite;
    @JsonProperty("logo")
    private String clientLogo;
    @JsonProperty("clientbusinessarea")
    private Map<String, Object> clientIndustry;
    @JsonProperty("address")
    private String clientAddress;
    @JsonProperty("description")
    private String clientDescription;
    @JsonProperty("industry")
    private String industry;
    @JsonProperty("owner")
    private Integer clientOwner;
    @JsonProperty("note")
    private List<SourceNote> note;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SourceContact {
        @JsonProperty("full_name")
        private String fullName;
        @JsonProperty("display_name")
        private String displayName;
        @JsonProperty("email")
        private String email;
        @JsonProperty("phone_number")
        private String phoneNumber;
        @JsonProperty("description")
        private String description;
        @JsonProperty("organization")
        private Integer organization;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SourceNote {
        @JsonProperty("info")
        private String content;
        private Integer creator;
        @JsonProperty("created_at")
        private String createdAt;
    }

}
