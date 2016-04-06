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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;
import java.util.stream.Stream;

import lombok.Data;

@Data
public class CfApplicationEntity {

    private JsonNode environmentJson;
    private String name;
    private String state;
    private String buildpack;
    private String detectedBuildpack;
    private String packageUpdatedAt;
    private Long memory;
    private UUID spaceGuid;
    private String command;
    private String detectedStartCommand;
    private Long instances;
    private Long diskQuota;
    private String healthCheckType;
    private Long healthCheckTimeout;

    public String getVersion() {
        return Stream.of(environmentJson.findPath("version"), environmentJson.findPath("VERSION"))
              .map(JsonNode::textValue)
              .filter(value -> value != null)
              .findFirst()
              .orElse(null);
    }
}
