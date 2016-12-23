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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.platformsnapshot.client.entity.TapService;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Data
@Table(name = "TAP_SERVICE_ARTIFACT")
@NoArgsConstructor
@Entity
public class TapServiceArtifact implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapServiceArtifact.class);

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SNAPSHOT_ID")
    @JsonIgnore
    private PlatformSnapshot snapshot;

    @Column(name="LABEL")
    private String label;
    @Column(name="DESCRIPTION")
    private String description;
    @Column(name="UPDATED_AT")
    private Date updatedAt;
    @Column(name="CREATED_AT")
    private Date createdAt;
    @Column(name="GUID")
    private String guid;

    public TapServiceArtifact(TapService tapService) {

            this.label = tapService.getName();
            this.description = tapService.getDescription();
            this.updatedAt = null;
            this.createdAt = null;
            this.guid = tapService.getId().toString();
            LOGGER.info("Creating service artifact: {} ", toString());

    }

    @Override
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        } else if(other == null) {
            return false;
        } else if(!other.getClass().equals(TapServiceArtifact.class)) {
            return false;
        }
        return Objects.equals(guid, ((TapServiceArtifact) other).getGuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(guid);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("label", label)
                .add("description", description)
                .add("updatedAt", updatedAt)
                .add("createdAt", createdAt)
                .add("guid", guid)
                .toString();
    }
}
