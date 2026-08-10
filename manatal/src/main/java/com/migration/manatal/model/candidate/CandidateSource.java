package com.migration.manatal.model.candidate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateSource {

    private Long id;
    @JsonProperty("external_id")
    private String externalId;
    @JsonProperty("full_name")
    private String fullName;
    private Long creator;
    private Integer owner;
    @JsonProperty("source_type")
    private String sourceType;
    @JsonProperty("source_other")
    private String sourceOther;
    private Boolean consent;
    @JsonProperty("consent_date")
    private String consentDate;
    private String picture;
    private String email;
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String gender;
    @JsonProperty("birth_date")
    private String birthDate;
    private String address;
    private String zipcode;
    @JsonProperty("candidate_location")
    private String candidateLocation;
    @JsonProperty("latest_degree")
    private String latestDegree;
    @JsonProperty("latest_university")
    private String latestUniversity;
    @JsonProperty("current_company")
    private String currentCompany;
    @JsonProperty("current_department")
    private String currentDepartment;
    @JsonProperty("current_position")
    private String currentPosition;
    private String description;
    @JsonProperty("candidate_tags")
    private List<CandidateTag> candidateTags;
    @JsonProperty("candidate_industries")
    private List<CandidateIndustry> candidateIndustries;
    private List<Skill> skills;
    private String hash;
    @JsonProperty("custom_fields")
    private CandidateCustomFields customFields;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandidateTag {
        private Long id;
        @JsonProperty("tag_id")
        private Long tagId;
        @JsonProperty("tag_name")
        private String tagName;
        @JsonProperty("tag_color")
        private String tagColor;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandidateIndustry {
        private Long id;
        private Long industry;
        @JsonProperty("industry_name")
        private String industryName;
        private Long candidate;
        @JsonProperty("creator_name")
        private String creatorName;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Skill {
        private Long id;
        private Long skill;
        @JsonProperty("skill_name")
        private String skillName;
        private Integer score;
        private String source;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandidateNote {
        private Long id;
        private String info;
        private Long creator;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Activity {
        private Long id;
        private String name;
        private String description;
        @JsonProperty("activity_type")
        private String activityType;
        @JsonProperty("due_date")
        private String dueDate;
        private Integer creator;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Nationality {
        private Long id;
        private String country;
        private Long candidate;
        @JsonProperty("creator_name")
        private String creatorName;
        private String nationality;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandidateMatch {
        private Long id;
        private Long job;
        private Long candidate;
        private Stage stage;
        @JsonProperty("is_active")
        private Boolean isActive;
        @JsonProperty("dropped_at")
        private String droppedAt;
        @JsonProperty("job_pipeline_stage")
        private PipelineStage jobPipelineStage;

        public String getStageName() {
            if (stage != null && stage.getName() != null) return stage.getName();
            if (jobPipelineStage != null && jobPipelineStage.getName() != null) return jobPipelineStage.getName();
            return null;
        }

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Stage {
            private Long id;
            private String name;
        }

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PipelineStage {
            private Long id;
            private String name;
            private Integer rank;
            @JsonProperty("job_pipeline")
            private Pipeline pipeline;

            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Pipeline {
                private Long id;
                private String name;
            }
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Resume {
        private Long id;
        @JsonProperty("resume_file")
        private String resumeFile;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SocialMedia {
        private Long id;
        @JsonProperty("social_media")
        private String socialMedia;
        @JsonProperty("social_media_url")
        private String socialMediaUrl;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Attachment {
        private Long id;
        private String name;
        private String description;
        private String file;
        private Long creator;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandidateCustomFields {
        private String technicaldomains;
        private List<String> businessdomains;
        private List<String> technologies;
        private String testlifylink;
        private List<String> english;
        private String french;
        private String spanish;
        private String portuguese;
        private String englishlevel;
        private String frenchlevel;
        private String spanishlevel;
        private String portugueselevel;
        private String ratehistory;
        private String candidatecertifications;
        @JsonProperty("documentaoregularizada")
        private Boolean documentaoregularizada;
        private String name;
        private String date;
        private String summary;
        private List<String> maintechnologies;
    }

}
