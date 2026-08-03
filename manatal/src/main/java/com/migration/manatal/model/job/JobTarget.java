package com.migration.manatal.model.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobTarget {

    @JsonProperty("position_name")
    private String positionName;
    private Integer organization;
    private Integer headcount;
    private Integer owner;
    @JsonProperty("contract_details")
    private String contractDetails;
    private String status;
    private String description;
    private String city;
    private String state;
    private String country;
    private String address;
    private String zipcode;
    @JsonProperty("is_remote")
    private Boolean isRemote;
    @JsonProperty("open_at")
    private String openAt;
    @JsonProperty("close_at")
    private String closeAt;
    private Integer industry;
    @JsonProperty("custom_fields")
    private JobCustomFields customFields;
    private List<TargetNote> notes;

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
    public static class JobCustomFields {
        @JsonProperty("businessunit")
        private String businessUnit;
        @JsonProperty("experiencelevelofficial")
        private List<String> experienceLevelOfficial;
        @JsonProperty("englishlevel")
        private String englishLevel;
        private String workplace;
        private String rate;
        private List<String> category;
        @JsonProperty("technicalskill")
        private List<String> technicalSkill;
        private List<String> portugus;
        @JsonProperty("officelocation")
        private List<String> officeLocation;
        @JsonProperty("jobadditionalinformation")
        private String jobAdditionalInformation;
        @JsonProperty("consultantname")
        private String consultantName;
        @JsonProperty("startdatejob")
        private String startDateJob;
        @JsonProperty("startdatesyffer")
        private String startDateSyffer;
        @JsonProperty("grossmargin")
        private Integer grossMargin;
        @JsonProperty("rateday")
        private Integer rateDay;
        @JsonProperty("costday")
        private Integer costDay;
        @JsonProperty("firstjobclosedinclient")
        private Boolean firstJobClosedInClient;
        @JsonProperty("morethanmonth")
        private Boolean moreThanMonth;
        @JsonProperty("ratehistory")
        private String rateHistory;
        private String internalization;
        private Boolean inherited;
        @JsonProperty("invoicepaymentterm")
        private String invoicePaymentTerm;
        @JsonProperty("purchaseorder")
        private String purchaseOrder;
        @JsonProperty("activebusiness")
        private Boolean activeBusiness;
        @JsonProperty("replacepreviousposition")
        private Boolean replacePreviousPosition;
        @JsonProperty("contactname")
        private String contactName;
    }

}
