package com.migration.manatal.transform;

import com.migration.manatal.model.ClientSource;
import com.migration.manatal.model.ClientTarget;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientTarget toTarget(ClientSource source) {
        if (source == null) return null;
        ClientTarget target = new ClientTarget();
        target.setClientName(source.getClientName());
        target.setClientWebsite(source.getClientWebsite());
        target.setClientIndustry(source.getClientIndustry());
        target.setClientLocation(source.getClientAddress());
        target.setClientDescription(source.getClientDescription());
        target.setClientOwner(source.getClientOwner());
        target.setTeamMembers(source.getTeamMembers());
        target.setStage(source.getStage());
        return target;
    }
}