package com.migration.manatal.batch.migration.client;
import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.model.client.ClientTarget;
import lombok.Data;

@Data
public class ClientMigrationPackage {

    private ClientMigration entity;
    private ClientTarget transformed;
    private Long targetClientId;
    private String errorMessage;

}
