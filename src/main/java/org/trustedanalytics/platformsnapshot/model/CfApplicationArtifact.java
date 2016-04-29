/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trustedanalytics.platformsnapshot.model;

import org.trustedanalytics.platformsnapshot.client.entity.CfApplication;
import org.trustedanalytics.platformsnapshot.client.entity.CfApplicationEntity;
import org.trustedanalytics.platformsnapshot.client.entity.CfMetadata;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class CfApplicationArtifact implements CfAccountArtifact {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, cascade = CascadeType.DETACH)
    @JsonIgnore
    private PlatformSnapshot snapshot;
    private UUID guid;
    private Date createdAt;
    private Date updatedAt;
    private UUID organization;
    private UUID space;
    private String name;
    private String version;
    private String buildpack;
    private String detectedBuildpack;
    private String command;
    private String detectedStartCommand;
    private String state;
    private Long memory;
    private Long instances;
    private Long diskQuota;
    private String healthCheckType;
    private Long healthCheckTimeout;
    private Scope scope;

    public CfApplicationArtifact(CfApplication cfApp, UUID organization, Scope scope) {
        final CfMetadata metadata = cfApp.getMetadata();
        final CfApplicationEntity entity = cfApp.getEntity();

        this.setScope(scope);
        this.setGuid(metadata.getGuid());
        this.setVersion(entity.getVersion());
        this.setCreatedAt(Date.from(metadata.getCreatedAt().toInstant(ZoneOffset.UTC)));
        this.setUpdatedAt(Optional.ofNullable(metadata.getUpdatedAt()).map(date -> Date.from(date.toInstant(ZoneOffset.UTC))).orElse(null));
        this.setName(entity.getName());
        this.setOrganization(organization);
        this.setSpace(entity.getSpaceGuid());
        this.setBuildpack(entity.getBuildpack());
        this.setDetectedBuildpack(entity.getDetectedBuildpack());
        this.setMemory(entity.getMemory());
        this.setInstances(entity.getInstances());
        this.setDiskQuota(entity.getDiskQuota());
        this.setState(entity.getState());
        this.setCommand(entity.getCommand());
        this.setDetectedStartCommand(entity.getDetectedStartCommand());
        this.setHealthCheckType(entity.getHealthCheckType());
        this.setHealthCheckTimeout(entity.getHealthCheckTimeout());
    }
}
