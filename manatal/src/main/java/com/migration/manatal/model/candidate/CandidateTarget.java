package com.migration.manatal.model.candidate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateTarget {

    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("address")
    private String candidateLocation;
    private String email;
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String description;
    private Boolean consent;
    private Integer owner;
    @JsonProperty("custom_fields")
    private CandidateCustomFields customFields;
    private List<TargetSkill> skills;
    private List<TargetNote> notes;
    private List<TargetNationality> nationalities;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TargetSkill {
        @JsonProperty("skill_name")
        private String skillName;
        private Integer score;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TargetNote {
        @JsonProperty("info")
        private String content;
        private Integer creator;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TargetNationality {
        private String country;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandidateCustomFields {
        private String technicaldomains;
        private List<String> businessdomains;
        private List<String> category;
        private String testlifylink;
        private String englishlevel;
        private String frenchlevel;
        private String spanishlevel;
        private String portugueselevel;
        private String ratehistory;
        private String candidatecertifications;
        @JsonProperty("workvisaeucitizenship")
        private Boolean workVisaEuCitizenship;
        @JsonProperty("informeonomedapessoaqueoindicou")
        private String referenceName;
        private String date;
        private List<String> technologies;
    }

}
