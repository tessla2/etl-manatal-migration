package com.migration.manatal.transform;

import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientSource.SourceNote;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.model.client.ClientTarget.ContactTarget;
import com.migration.manatal.model.client.ClientTarget.TargetNote;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientMapper {

    private static final int FIXED_OWNER_ID = 1193857;
    private static final String FIXED_CREATOR_ID = "1193857";

    public ClientTarget toTarget(ClientSource source) {
        return toTarget(source, List.of(), List.of());
    }

    public ClientTarget toTarget(ClientSource source, List<ClientSource.SourceContact> contacts, List<SourceNote> notes) {
        if (source == null) return null;
        ClientTarget target = new ClientTarget();
        target.setClientName(source.getClientName());
        target.setClientWebsite(source.getClientWebsite());
        target.setClientLogo(source.getClientLogo());
        target.setClientIndustry(source.getClientIndustry());
        target.setClientLocation(source.getClientAddress());
        target.setClientDescription(source.getClientDescription());
        target.setClientOwner(FIXED_OWNER_ID);

        target.setContacts(contacts.stream().map(c -> {
            ContactTarget ct = new ContactTarget();
            ct.setFullName(c.getFullName());
            ct.setDisplayName(c.getDisplayName());
            ct.setEmail(c.getEmail());
            ct.setPhoneNumber(c.getPhoneNumber());
            ct.setDescription(c.getDescription());
            ct.setCustomFields(c.getCustomFields());
            if (c.getId() != null) {
                ct.setSourceContactId(c.getId().longValue());
            }
            return ct;
        }).toList());

        target.setNotes(notes.stream().map(n -> {
            TargetNote nt = new TargetNote();
            nt.setContent(n.getContent());
            nt.setCreator(FIXED_CREATOR_ID);
            nt.setCreatedAt(n.getCreatedAt());
            nt.setCreatorName(n.getCreatorName());
            if (n.getContactId() != null) {
                nt.setContactId(n.getContactId().longValue());
            }
            return nt;
        }).toList());

        return target;
    }
}