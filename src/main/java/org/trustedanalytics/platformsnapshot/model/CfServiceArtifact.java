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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.platformsnapshot.client.entity.CfService;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Data
@Table(name = "CF_SERVICE_ARTIFACT")
@NoArgsConstructor
@Entity
public class CfServiceArtifact {

    private static final Logger LOGGER = LoggerFactory.getLogger(CfServiceArtifact.class);

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "snapshot_id")
    @JsonIgnore
    private PlatformSnapshot snapshot;
    private String label;
    private String description;
    private Date updatedAt;
    private Date createdAt;
    private UUID guid;

    public CfServiceArtifact(CfService cfService) {

            this.label = cfService.getEntity().getLabel();
            this.description = cfService.getEntity().getDescription();
            this.updatedAt = Optional.ofNullable(cfService.getMetadata().getUpdatedAt()).map(date -> Date.from(date.toInstant(ZoneOffset.UTC))).orElse(null);
            this.createdAt = Date.from(cfService.getMetadata().getCreatedAt().toInstant(ZoneOffset.UTC));
            this.guid = cfService.getMetadata().getGuid();
    }
}
