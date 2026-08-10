package com.migration.manatal.transform;

import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.model.candidate.CandidateTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CandidateMapper {

    private final OwnerMapper ownerMapper;

    public CandidateTarget toTarget(CandidateSource source) {
        return toTarget(source, List.of(), List.of(), List.of());
    }

    public CandidateTarget toTarget(CandidateSource source, List<CandidateSource.CandidateNote> notes,
                                    List<CandidateSource.Nationality> nationalities) {
        return toTarget(source, notes, nationalities, List.of());
    }

    public CandidateTarget toTarget(CandidateSource source, List<CandidateSource.CandidateNote> notes,
                                    List<CandidateSource.Nationality> nationalities,
                                    List<CandidateSource.Skill> skills) {
        if (source == null) return null;

        CandidateTarget target = new CandidateTarget();
        target.setFullName(source.getFullName());
        target.setCandidateLocation(source.getCandidateLocation());
        target.setEmail(source.getEmail());
        target.setPhoneNumber(source.getPhoneNumber());
        target.setDescription(description(source));
        target.setConsent(source.getConsent());
        target.setOwner(ownerMapper.resolve(source.getOwner()));
        target.setCustomFields(mapCustomFields(source.getCustomFields()));

        target.setSkills(skills == null ? List.of() : skills.stream().map(this::mapSkill).toList());
        target.setNotes(notes == null ? List.of() : notes.stream().map(this::mapNote).toList());
        target.setNationalities(nationalities == null ? List.of()
                : nationalities.stream().map(this::mapNationality).toList());
        return target;
    }

    private CandidateTarget.TargetSkill mapSkill(CandidateSource.Skill skill) {
        CandidateTarget.TargetSkill target = new CandidateTarget.TargetSkill();
        target.setSkillName(skill.getSkillName());
        target.setScore(skill.getScore());
        return target;
    }

    private CandidateTarget.TargetNote mapNote(CandidateSource.CandidateNote note) {
        CandidateTarget.TargetNote target = new CandidateTarget.TargetNote();
        target.setContent(note.getInfo());
        target.setCreatedAt(note.getCreatedAt());
        return target;
    }

    private CandidateTarget.TargetNationality mapNationality(CandidateSource.Nationality nationality) {
        CandidateTarget.TargetNationality target = new CandidateTarget.TargetNationality();
        target.setCountry(nationality.getCountry());
        return target;
    }

    private CandidateTarget.CandidateCustomFields mapCustomFields(CandidateSource.CandidateCustomFields source) {
        CandidateTarget.CandidateCustomFields target = new CandidateTarget.CandidateCustomFields();
        if (source == null) return target;

        target.setTechnicaldomains(source.getTechnicaldomains());
        target.setBusinessdomains(source.getBusinessdomains());
        target.setCategory(source.getTechnologies());
        target.setTestlifylink(source.getTestlifylink());
        target.setEnglishlevel(level(source.getEnglish(), source.getEnglishlevel()));
        target.setFrenchlevel(level(source.getFrench(), source.getFrenchlevel()));
        target.setSpanishlevel(level(source.getSpanish(), source.getSpanishlevel()));
        target.setPortugueselevel(level(source.getPortuguese(), source.getPortugueselevel()));
        target.setRatehistory(source.getRatehistory());
        target.setCandidatecertifications(source.getCandidatecertifications());
        target.setWorkVisaEuCitizenship(source.getDocumentaoregularizada());
        target.setReferenceName(source.getName());
        target.setDate(source.getDate());
        target.setTechnologies(source.getMaintechnologies());

        return target;
    }

    private String level(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue : oldValue;
    }

    private String level(List<String> newValue, String oldValue) {
        if (newValue != null && !newValue.isEmpty()) return newValue.get(0);
        return oldValue;
    }

    private String description(CandidateSource source) {
        CandidateSource.CandidateCustomFields customFields = source.getCustomFields();
        if (customFields != null && customFields.getSummary() != null && !customFields.getSummary().isBlank()) {
            return stripHtml(customFields.getSummary());
        }
        return stripHtml(source.getDescription());
    }

    private String stripHtml(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("<[^>]+>", "").trim();
    }

}
