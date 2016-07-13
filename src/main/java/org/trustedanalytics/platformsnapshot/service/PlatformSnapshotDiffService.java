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
package org.trustedanalytics.platformsnapshot.service;


import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import org.trustedanalytics.platformsnapshot.service.diff.FlattenDiffProcessor;
import org.trustedanalytics.platformsnapshot.service.diff.IDiffProcessor;
import org.trustedanalytics.platformsnapshot.service.diff.PartitionedDiffProcessor;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class PlatformSnapshotDiffService {

    private final PlatformSnapshotRepository repository;

    @Autowired
    public PlatformSnapshotDiffService(PlatformSnapshotRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public PlatformSnapshotDiff diff(Optional<Long> idBefore, Optional<Long> idAfter) {
        return process(idBefore, idAfter, new FlattenDiffProcessor());
    }

    public PlatformSnapshotDiff diffByType(Optional<Long> idBefore, Optional<Long> idAfter) {
        return process(idBefore, idAfter, new PartitionedDiffProcessor());
    }

    private PlatformSnapshotDiff process(Optional<Long> idBefore, Optional<Long> idAfter, IDiffProcessor processor) {
        if (!idAfter.isPresent() && !idBefore.isPresent()) {
            throw new IllegalArgumentException("No snapshot was provided");
        }

        final PlatformSnapshot before = idBefore.map(this::findSnapshot)
                .orElseThrow(() -> new IllegalArgumentException("Before snapshot was not provided"));
        final PlatformSnapshot after = idAfter.map(this::findSnapshot).orElse(getLatest());

        final DiffNode root = ObjectDifferBuilder.buildDefault().compare(after, before);
        return processor.process(root, before, after);
    }

    private PlatformSnapshot getLatest() {
        PlatformSnapshot latest = repository.findTopByOrderByCreatedAtDesc();
        checkNotNull(latest, "Cannot find any snapshot");
        return repository.findTopByOrderByCreatedAtDesc();
    }

    private PlatformSnapshot findSnapshot(Long id) {
        checkArgument(repository.exists(id), String.format("Snapshot with id %s does not exist", id));
        return repository.findOne(id);
    }
}