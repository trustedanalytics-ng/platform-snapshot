/**
 * Copyright (c) 2016 Intel Corporation
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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartitionedPlatformSnapshotDiff implements PlatformSnapshotDiff{

    @JsonProperty
    private Date createdAtBefore;
    @JsonProperty
    private Date createdAtAfter;
    @JsonProperty
    private Collection<PlatformSnapshotDiffEntry> applications;
    @JsonProperty
    private Collection<PlatformSnapshotDiffEntry> tapServices;
    @JsonProperty
    private Collection<PlatformSnapshotDiffEntry> cdhServices;
}