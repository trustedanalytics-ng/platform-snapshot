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

import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table(name = "CF_APPLICATION_ARTIFACT")
@NoArgsConstructor
@Entity
public class CfApplicationArtifact implements  Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SNAPSHOT_ID")
    @JsonIgnore
    private PlatformSnapshot snapshot;
    @Column(name="GUID")
    private String guid;
    @Column(name="CREATED_AT")
    private Date createdAt;
    @Column(name="UPDATED_AT")
    private Date updatedAt;
    @Column(name="ORGANIZATION")
    private String organization;
    @Column(name="SPACE")
    private String space;
    @Column(name="NAME")
    private String name;
    @Column(name="VERSION")
    private String version;
    @Column(name="BUILDPACK")
    private String buildpack;
    @Column(name="DETECTED_BUILDPACK")
    private String detectedBuildpack;
    @Column(name="COMMAND")
    private String command;
    @Column(name="DETECTED_START_COMMAND")
    private String detectedStartCommand;
    @Column(name="STATE")
    private String state;
    @Column(name="MEMORY")
    private Long memory;
    @Column(name="INSTANCES")
    private Long instances;
    @Column(name="DISK_QUOTA")
    private Long diskQuota;
    @Column(name="HEALTH_CHECK_TYPE")
    private String healthCheckType;
    @Column(name="HEALTH_CHECK_TIMEOUT")
    private Long healthCheckTimeout;
    @Column(name="SCOPE")
    private String scope;

    public CfApplicationArtifact(CfApplication cfApp, UUID organization, String scope) {
        final CfMetadata metadata = cfApp.getMetadata();
        final CfApplicationEntity entity = cfApp.getEntity();

        this.setScope(scope);
        this.setGuid(metadata.getGuid().toString());
        this.setVersion(entity.getVersion());
        this.setCreatedAt(Date.from(metadata.getCreatedAt().toInstant(ZoneOffset.UTC)));
        this.setUpdatedAt(Optional.ofNullable(metadata.getUpdatedAt()).map(date -> Date.from(date.toInstant(ZoneOffset.UTC))).orElse(null));
        this.setName(entity.getName());
        this.setOrganization(organization.toString());
        this.setSpace(entity.getSpaceGuid().toString());
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
