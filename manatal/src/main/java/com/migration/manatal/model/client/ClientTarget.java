package com.migration.manatal.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
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

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactTarget {
        private Long id;
        private String fullName;
        private String displayName;
        private String email;
        private String phoneNumber;
        private String description;
        private Long organization;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TargetNote {
        private String content;
        private String creator;
        private String createdAt;
    }

    private List<ContactTarget> contacts;

    private List<TargetNote> notes;

}
