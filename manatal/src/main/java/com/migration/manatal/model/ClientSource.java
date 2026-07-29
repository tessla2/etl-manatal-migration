package com.migration.manatal.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientSource {


    private String clientName;
    private String clientWebsite;
    private Map<String, Object> clientIndustry;
    private String clientAddress;
    private String clientDescription;
    private Integer clientOwner;
    private String teamMembers;
    private String stage;
    private List<ManatalNote> note;

    @Data
    public static class ManatalContact {
        private Long id;
        private String fullName;
        private String displayName;
        private String email;
        private String phoneNumber;
        private String description;
        private Long organization;
    }


    @Data
    public static class ManatalNote {
        private String content;
        private String creator;
        private String created_at;
    }

}
