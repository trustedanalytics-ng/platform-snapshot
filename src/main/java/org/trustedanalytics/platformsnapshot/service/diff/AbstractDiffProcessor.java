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

import com.google.common.collect.ImmutableSet;
import de.danielbechler.diff.node.DiffNode;
import org.springframework.security.util.FieldUtils;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.CfApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiffEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public abstract class AbstractDiffProcessor {

    private static final Set<Class<?>> ARTIFACT_TYPES = ImmutableSet.of(CfApplicationArtifact.class, CdhServiceArtifact.class);

    protected Collection<PlatformSnapshotDiffEntry> processChanges(DiffNode root, PlatformSnapshot after, PlatformSnapshot before) {
        Collection<PlatformSnapshotDiffEntry> diffs = new ArrayList<>();
        root.visit((node, visit) -> {
            if (hasMetricChanged(node)) {
                Object target = resolveChangedMetric(node, after, before);
                final Object artifact = resolveArtifactName(node, target);

                if (artifact != null) {

                    PlatformSnapshotDiffEntry diff = new PlatformSnapshotDiffEntry();
                    diff.setArtifact(artifact.toString());
                    diff.setMetric(node.getPropertyName());
                    diff.setOperation(node.getState().toString());
                    diff.setType(node.getParentNode().getValueType());
                    diff.setAfter(node.canonicalGet(after));
                    diff.setBefore(node.canonicalGet(before));

                    if (!isMetricComparable(node)) {
                        diffs.add(diff);
                    }
                }
            }
        });

        return diffs;
    }

    private boolean isMetricComparable(DiffNode node) { return "id".equals(node.getPropertyName()); }

    private boolean hasMetricChanged(DiffNode node) {
        return node.hasChanges() && !node.hasChildren();
    }

    private Object resolveArtifactName(DiffNode node, Object target) {
        return ARTIFACT_TYPES.contains(node.getParentNode().getValueType()) ? resolveField(target, "name") : resolveField(target, "label");
    }

    private Object resolveChangedMetric(DiffNode node, PlatformSnapshot after, PlatformSnapshot before) {
        return DiffNode.State.ADDED.equals(node.getState()) ? node.getParentNode().canonicalGet(after) :
                node.getParentNode().canonicalGet(before);
    }

    private Object resolveField(Object object, String fieldName) {
        try {
            return FieldUtils.getFieldValue(object, fieldName);
        } catch (IllegalAccessException | IllegalStateException e) {
            // ignore - we need to query object for field
            return null;
        }
    }
}