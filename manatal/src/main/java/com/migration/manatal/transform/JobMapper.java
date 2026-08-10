package com.migration.manatal.transform;

import com.migration.manatal.model.job.JobSource;
import com.migration.manatal.model.job.JobTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobMapper {

    private static final String BUSINESS_UNIT = "PT - IT";

    private final OwnerMapper ownerMapper;
    private final IndustryMapper industryMapper;

    public JobTarget toTarget(JobSource source) {
        return toTarget(source, List.of());
    }

    public JobTarget toTarget(JobSource source, List<JobSource.JobNote> notes) {
        if (source == null) return null;

        JobTarget target = new JobTarget();
        target.setPositionName(source.getPositionName());
        target.setOrganization(source.getOrganization());
        target.setHeadcount(source.getHeadcount());
        target.setOwner(ownerMapper.resolve(source.getOwner()));
        target.setContractDetails(source.getContractDetails());
        target.setStatus(source.getStatus());
        target.setDescription(source.getDescription());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setCountry(source.getCountry());
        target.setCurrency("EUR");
        target.setAddress(source.getAddress());
        target.setZipcode(source.getZipcode());
        target.setIsRemote(source.getIsRemote());
        target.setOpenAt(dateOnly(source.getOpenAt()));
        target.setCloseAt(dateOnly(source.getCloseAt()));

        if (source.getIndustry() != null) {
            target.setIndustry(industryMapper.resolve(source.getIndustry().getName()));
        }

        target.setCustomFields(mapCustomFields(source.getCustomFields()));
        target.setNotes(notes == null ? List.of() : notes.stream().map(this::mapNote).toList());
        return target;
    }

    private JobTarget.TargetNote mapNote(JobSource.JobNote note) {
        JobTarget.TargetNote target = new JobTarget.TargetNote();
        target.setContent(note.getContent());
        target.setCreator(note.getCreator());
        target.setCreatedAt(note.getCreatedAt());
        return target;
    }

    private JobTarget.JobCustomFields mapCustomFields(JobSource.JobCustomFields source) {
        JobTarget.JobCustomFields target = new JobTarget.JobCustomFields();
        target.setBusinessUnit(BUSINESS_UNIT);
        if (source == null) return target;

        target.setExperienceLevel(source.getExperienceLevelOfficial());
        target.setEnglishLevel(source.getEnglishLevel());
        target.setWorkplace(source.getJobModel());
        target.setRate(stripHtml(source.getClientRate()));
        target.setCategory(source.getCategory());
        target.setMandatorySkills(source.getTechnicalSkill());
        target.setPortugus(source.getPortugus());
        target.setOfficeLocation(source.getOfficeLocation());
        target.setJobAdditionalInformation(source.getJobModelDetails());
        target.setProjectNotes(source.getProjectNotes());
        target.setSkillNotes(joinList(source.getTechnologies()));
        target.setLostReason(source.getLostReason());

        target.setConsultantName(source.getConsultantName());
        target.setStartDateJob(source.getStartDateOportunity() != null ? source.getStartDateOportunity() : source.getStartDateJob());
        target.setStartDateSyffer(source.getStartDateSyffer());
        target.setGrossMargin(source.getGrossMargin());
        target.setRateDay(source.getRateDay());
        target.setCostDay(source.getCostDay());
        target.setFirstJobClosedInClient(source.getFirstJobClosedInClient());
        target.setMoreThanMonth(source.getMoreThanMonth());
        target.setInternalization(source.getInternalization());
        target.setInherited(source.getInherited());
        target.setInvoicePaymentTerm(source.getInvoicePaymentTerm());
        target.setPurchaseOrder(source.getPurchaseOrder());
        target.setActiveBusiness(source.getActiveBusiness());
        target.setReplacePreviousPosition(source.getReplacePreviousPosition());
        target.setContactName(source.getContactName());

        return target;
    }

    private String dateOnly(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) return null;
        int index = dateTime.indexOf('T');
        return index >= 0 ? dateTime.substring(0, index) : dateTime;
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        return String.join(", ", values);
    }

    private String stripHtml(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("<[^>]+>", "").trim();
    }

}
