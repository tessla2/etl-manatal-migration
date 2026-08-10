package com.migration.manatal.model.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobSource {

    private Integer id;
    @JsonProperty("external_id")
    private String externalId;
    private String hash;
    private Integer organization;
    @JsonProperty("position_name")
    private String positionName;
    private String description;
    private Integer headcount;
    private Integer creator;
    @JsonProperty("salary_min")
    private Integer salaryMin;
    @JsonProperty("salary_max")
    private Integer salaryMax;
    private String currency;
    private Integer owner;
    private String address;
    private String zipcode;
    @JsonProperty("contract_details")
    private String contractDetails;
    @JsonProperty("is_published")
    private Boolean isPublished;
    @JsonProperty("is_remote")
    private Boolean isRemote;
    private String status;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("career_page_url")
    private String careerPageUrl;
    @JsonProperty("custom_fields")
    private JobCustomFields customFields;
    @JsonProperty("is_pinned_in_career_page")
    private Boolean isPinnedInCareerPage;
    private Industry industry;
    private String frequency;
    private String city;
    private String state;
    private String country;
    @JsonProperty("open_at")
    private String openAt;
    @JsonProperty("close_at")
    private String closeAt;
    @JsonProperty("expected_close_at")
    private String expectedCloseAt;
    @JsonProperty("is_salary_visible")
    private Boolean isSalaryVisible;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobCustomFields {
        @JsonProperty("clientrate")
        private String clientRate;
        @JsonProperty("costday")
        private Integer costDay;
        @JsonProperty("rateday")
        private Integer rateDay;
        @JsonProperty("categoy")
        private List<String> category;
        @JsonProperty("portugus")
        private List<String> portugus;
        @JsonProperty("inherited")
        private Boolean inherited;
        @JsonProperty("jobmodel")
        private String jobModel;
        @JsonProperty("atualstatus")
        private String atualStatus;
        @JsonProperty("contactname")
        private String contactName;
        @JsonProperty("grossmargin")
        private Integer grossMargin;
        @JsonProperty("ratehistory")
        private String rateHistory;
        @JsonProperty("businessunit")
        private String businessUnit;
        @JsonProperty("englishlevel")
        private String englishLevel;
        @JsonProperty("startdatejob")
        private String startDateJob;
        @JsonProperty("morethanmonth")
        private Boolean moreThanMonth;
        @JsonProperty("purchaseorder")
        private String purchaseOrder;
        @JsonProperty("activebusiness")
        private Boolean activeBusiness;
        @JsonProperty("consultantname")
        private String consultantName;
        @JsonProperty("headcountatual")
        private Integer headcountAtual;
        @JsonProperty("officelocation")
        private List<String> officeLocation;
        @JsonProperty("technicalskill")
        private List<String> technicalSkill;
        @JsonProperty("technologies")
        private List<String> technologies;
        @JsonProperty("startdateoportunity")
        private String startDateOportunity;
        @JsonProperty("internalization")
        private String internalization;
        @JsonProperty("startdatesyffer")
        private String startDateSyffer;
        @JsonProperty("invoicepaymentterm")
        private String invoicePaymentTerm;
        @JsonProperty("firstjobclosedinclient")
        private Boolean firstJobClosedInClient;
        @JsonProperty("experiencelevelofficial")
        private List<String> experienceLevelOfficial;
        @JsonProperty("replacepreviousposition")
        private Boolean replacePreviousPosition;
        @JsonProperty("jobmodeldetails")
        private String jobModelDetails;
        @JsonProperty("projectnotes")
        private String projectNotes;
        @JsonProperty("lostreason")
        private String lostReason;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Industry {
        private Integer id;
        private String name;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobNote {
        private Integer id;
        @JsonProperty("info")
        private String content;
        private Integer creator;
        @JsonProperty("created_at")
        private String createdAt;
    }

}
