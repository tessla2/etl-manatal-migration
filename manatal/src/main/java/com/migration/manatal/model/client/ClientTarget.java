package com.migration.manatal.model.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientTarget {

    @JsonProperty("name")
    private String clientName;
    @JsonProperty("website")
    private String clientWebsite;
    @JsonProperty("logo")
    private String clientLogo;
    @JsonProperty("clientbusinessarea")
    private Map<String, Object> clientIndustry;
    @JsonProperty("custom_fields")
    private Map<String, Object> customFields;
    @JsonProperty("address")
    private String clientLocation;
    @JsonProperty("description")
    private String clientDescription;
    @JsonProperty("owner")
    private Integer clientOwner;
    @JsonProperty("team_members")
    private String teamMembers;
    @JsonProperty("stage")
    private String stage;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactTarget {
        @JsonProperty("id")
        private Long id;
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
        private Long organization;
        @JsonProperty("custom_fields")
        private Map<String, Object> customFields;
        @JsonIgnore
        private Long sourceContactId;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TargetNote {
        @JsonProperty("info")
        private String content;
        @JsonProperty("creator")
        private String creator;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonIgnore
        private Long contactId;
        @JsonIgnore
        private String creatorName;
    }

    private List<ContactTarget> contacts;

    private List<TargetNote> notes;

}
