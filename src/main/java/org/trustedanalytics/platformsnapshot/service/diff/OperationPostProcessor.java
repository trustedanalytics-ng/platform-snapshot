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

import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.TapApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.TapServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiffEntry;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.danielbechler.diff.node.DiffNode.State;

/**
 * Processor recognizes when component was added or removed from the platform
 * and modifies operations accordingly. This resolve the problem when metric
 * changes value from null to nonnull and other way around.
 *
 * Without knowing the context it's not possible to tell if null represent
 * real value or lack of value. In other words change from null to nonnull
 * does not always mean that metric was added.
 *
 * In case of cloud foundry component we can recognize whether component was
 * added or removed by looking at guid. In case of hadoop we do the same using
 * service name.
 */
public class OperationPostProcessor implements DiffPostProcessor {
    private static final Set<Class<?>> TAP_ARTIFACTS = ImmutableSet.of(TapApplicationArtifact.class, TapServiceArtifact.class);
    private static final Set<Class<?>> CDH_ARTIFACTS = ImmutableSet.of(CdhServiceArtifact.class);

    @Override
    public void accept(Collection<PlatformSnapshotDiffEntry> diffs) {
        diffs.stream()
            .collect(Collectors.groupingBy(e -> e.getType().getSimpleName() + e.getArtifact()))
            .forEach((key, component) -> {
                if (isComponentAdded(component)) {
                    component.forEach(diff -> diff.setOperation(State.ADDED.toString()));
                } else if (isComponentRemoved(component)) {
                    component.forEach(diff -> diff.setOperation(State.REMOVED.toString()));
                } else {
                    component.forEach(diff -> diff.setOperation(State.CHANGED.toString()));
                }
            });
    }

    private boolean isComponentAdded(List<PlatformSnapshotDiffEntry> componentDiff) {
        return componentDiff.stream()
            .filter(c -> isK8SComponentId(c) || isHadoopComponentId(c))
            .filter(c -> c.getBefore() == null)
            .filter(c -> c.getAfter() != null)
            .findAny()
            .isPresent();
    }

    private boolean isComponentRemoved(List<PlatformSnapshotDiffEntry> componentDiff) {
        return componentDiff.stream()
            .filter(c -> isK8SComponentId(c) || isHadoopComponentId(c))
            .filter(c -> c.getBefore() != null)
            .filter(c -> c.getAfter() == null)
            .findAny()
            .isPresent();
    }

    private boolean isK8SComponentId(PlatformSnapshotDiffEntry diff) {
        return TAP_ARTIFACTS.contains(diff.getType()) && "guid".equalsIgnoreCase(diff.getMetric());
    }

    private boolean isHadoopComponentId(PlatformSnapshotDiffEntry diff) {
        return CDH_ARTIFACTS.contains(diff.getType()) && "name".equalsIgnoreCase(diff.getMetric());
    }
}
