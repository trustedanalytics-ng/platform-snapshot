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
import org.trustedanalytics.platformsnapshot.model.FlattenPlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiff;

public class FlattenDiffProcessor extends AbstractDiffProcessor implements DiffProcessor {
    @Override
    public PlatformSnapshotDiff process(DiffNode root, PlatformSnapshot before, PlatformSnapshot after) {
        return FlattenPlatformSnapshotDiff.builder()
                .components(processChanges(root, after, before))
                .createdAtAfter(after.getCreatedAt())
                .createdAtBefore(before.getCreatedAt()).build();
    }
}