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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import org.trustedanalytics.platformsnapshot.service.diff.FlattenDiffProcessor;
import org.trustedanalytics.platformsnapshot.service.diff.DiffProcessor;
import org.trustedanalytics.platformsnapshot.service.diff.PartitionedDiffProcessor;

import java.util.Objects;
import java.util.Optional;


@Service
public class PlatformSnapshotDiffService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSnapshotDiffService.class);

    private final PlatformSnapshotRepository repository;

    @Autowired
    public PlatformSnapshotDiffService(PlatformSnapshotRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public PlatformSnapshotDiff diff(long idBefore, long idAfter) {
        LOGGER.info("Comparing snapshots {} {}", idBefore, idAfter);
        return process(idBefore, idAfter, new FlattenDiffProcessor());
    }

    public PlatformSnapshotDiff diffByType(long idBefore, long idAfter) {
        LOGGER.info("Comparing snapshots {} {}", idBefore, idAfter);
        return process(idBefore, idAfter, new PartitionedDiffProcessor());
    }

    private PlatformSnapshotDiff process(long idBefore, long idAfter, DiffProcessor processor) {
        final PlatformSnapshot before = findSnapshot(idBefore);
        final PlatformSnapshot after = findSnapshot(idAfter);
        final DiffNode root = ObjectDifferBuilder.buildDefault().compare(after, before);

        LOGGER.info("Comparing snapshots {} {}", before.getCreatedAt(), after.getCreatedAt());
        return processor.process(root, before, after);
    }

    private PlatformSnapshot findSnapshot(long id) {
        return Optional.ofNullable(repository.findOne(id))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Snapshot with id %s does not exist", id)));
    }
}