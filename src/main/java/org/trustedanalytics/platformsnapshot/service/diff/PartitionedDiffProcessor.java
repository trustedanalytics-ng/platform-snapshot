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
package org.trustedanalytics.platformsnapshot.service.diff;

import de.danielbechler.diff.node.DiffNode;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.CfApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.CfServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.PartitionedPlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiffEntry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PartitionedDiffProcessor extends AbstractDiffProcessor implements DiffProcessor {
    @Override
    public PlatformSnapshotDiff process(DiffNode root, PlatformSnapshot before, PlatformSnapshot after) {
        final Collection<PlatformSnapshotDiffEntry> diffs = processDiffs(root, after, before);

        final Map<Class<?>, List<PlatformSnapshotDiffEntry>> entriesByType = diffs.stream()
                .collect(Collectors.groupingBy(PlatformSnapshotDiffEntry::getType, Collectors.toList()));

        return PartitionedPlatformSnapshotDiff.builder()
                .applications(entriesByType.get(CfApplicationArtifact.class))
                .cdhServices(entriesByType.get(CdhServiceArtifact.class))
                .cfServices(entriesByType.get(CfServiceArtifact.class))
                .createdAtAfter(after.getCreatedAt())
                .createdAtBefore(before.getCreatedAt())
                .build();
    }
}