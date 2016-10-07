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
package org.trustedanalytics.platformsnapshot.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;

@Data
public class TapApplication {
    private Long runningInstances;
    private String diskQuota;
    private String memory;
    private String imageType;
    private Set<String> urls;
    private String imageState;
    private String replication;
    private String id;
    private String name;
    private String type;
    private String ClassId;
    private String state;
    private AppMetadataEntry metadata[];

    @JsonProperty("auditTrail")
    private AuditTrail auditTrail;
}
