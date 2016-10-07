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

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.platformsnapshot.client.entity.AppMetadataEntry;
import org.trustedanalytics.platformsnapshot.client.entity.TapApplication;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.trustedanalytics.platformsnapshot.client.entity.TapMetadata;

@Data
@Table(name = "TAP_APPLICATION_ARTIFACT")
@NoArgsConstructor
@Entity
public class TapApplicationArtifact implements  Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TapApplicationArtifact.class);
    public static final String IMAGE_ADDRESS_KEY = "APPLICATION_IMAGE_ADDRESS";
    public static final String URLS_KEY = "urls";

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
    @Column(name="NAME")
    private String name;
    @Column(name="VERSION")
    private String version;
    @Column(name="STATE")
    private String state;
    @Column(name="MEMORY")
    private Long memory;
    @Column(name="INSTANCES")
    private Long instances;
    @Column(name="DISK_QUOTA")
    private Long diskQuota;
    @Column(name="IMAGE_ADDRESS")
    private String imageAddress;
    @Column(name="URLS")
    private String urls;
    @Column(name="CREATED_BY")
    private String createdBy;
    @Column(name="UPDATED_BY")
    private String updatedBy;
    @Column(name="IMAGE_TYPE")
    private String imageType;


    public TapApplicationArtifact(TapApplication tapApp) {
        LOGGER.info("Creating application artifact from{}", tapApp);
        this.setGuid(tapApp.getId().toString());

        this.setUpdatedAt(
                Optional.ofNullable(
                        Instant.ofEpochMilli(tapApp.getAuditTrail().getLastUpdatedOn()*1000)
                ).map(date -> Date.from(date)).orElse(null)
        );

        this.setCreatedAt(
                Optional.ofNullable(
                        Instant.ofEpochMilli(tapApp.getAuditTrail().getCreatedOn()*1000)
                ).map(date -> Date.from(date)).orElse(null)
        );

        this.setName(tapApp.getName());
        this.setMemory(getMegabytes(tapApp.getMemory()));
        this.setInstances(tapApp.getRunningInstances());
        this.setDiskQuota(getMegabytes(tapApp.getDiskQuota()));
        this.setState(tapApp.getState());
        this.setCreatedBy(tapApp.getAuditTrail().getCreatedBy());
        this.setUpdatedBy(tapApp.getAuditTrail().getLastUpdateBy());
        this.setImageAddress(getMetadata(tapApp, IMAGE_ADDRESS_KEY));
        this.setUrls(getMetadata(tapApp, URLS_KEY));
        this.setImageType(tapApp.getImageType());

        LOGGER.info("Application artifact {}", toString());
    }

    @Override
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        } else if(other == null) {
            return false;
        } else if(!other.getClass().equals(TapApplicationArtifact.class)) {
            return false;
        }
        return Objects.equals(guid, ((TapApplicationArtifact) other).guid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(guid);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("guid", guid)
                .add("createdAt", createdAt)
                .add("updatedAt", updatedAt)
                .add("name", name)
                .add("version", version)
                .add("state", state)
                .add("memory", memory)
                .add("instances", instances)
                .add("diskQuota", diskQuota)
                .add("imageAddress",imageAddress)
                .add("urls",urls)
                .add("createdBy",createdBy)
                .add("updatedBy",updatedBy)
                .add("imageType",imageType)
                .toString();
    }

    private Long getMegabytes(String mb) {
        String values[] =mb.split("MB");
        if (values.length > 0) {
            return Long.valueOf(
                    values[0]
            );
        }
        return null;
    }

    private String getMetadata(TapApplication app, String key) {
        LOGGER.info("metadata {}", app.getMetadata());
        if (app == null || app.getMetadata() == null) {
            return null;
        }
        List<AppMetadataEntry> myData = Arrays.asList(app.getMetadata())
                .stream().filter(entry -> key.equals(entry.getKey()))
                 .collect(Collectors.toList());
        if (myData.size() > 0) {
            return myData.get(0).getValue();
        }
        return null;
    }
}
