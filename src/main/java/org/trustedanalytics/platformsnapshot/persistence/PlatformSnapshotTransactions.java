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
package org.trustedanalytics.platformsnapshot.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class PlatformSnapshotTransactions {

    final PlatformSnapshotRepository platformSnapshotRepository;

    @Autowired
    public PlatformSnapshotTransactions(PlatformSnapshotRepository platformSnapshotRepository) {
        this.platformSnapshotRepository = platformSnapshotRepository;
    }

    public void deleteOlderThen(Date date) {
        platformSnapshotRepository.deleteApplicationArtifacts(date);
        platformSnapshotRepository.deleteCfServiceArtifact(date);
        platformSnapshotRepository.deleteCdhServiceArtifact(date);
        platformSnapshotRepository.deletePlatformSnapshotsOlderThen(date);
    }
}
