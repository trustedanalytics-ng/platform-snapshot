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

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;

import java.util.Collection;
import java.util.Date;

@Repository
public interface PlatformSnapshotRepository extends CrudRepository<PlatformSnapshot, Long> {

    @Query("select p from PlatformSnapshot p where p.createdAt between ?1 and ?2 order by p.createdAt desc")
    Collection<PlatformSnapshot> findByDates(Date from, Date to);

    PlatformSnapshot findTopByOrderByCreatedAtDesc();

    @Modifying
    @Transactional
    @Query("delete from PlatformSnapshot p where p.createdAt <= ?1")
    void deletePlatformSnapshotsOlderThen(Date date);

    @Modifying
    @Transactional
    @Query("delete from CfApplicationArtifact a where a.snapshot.createdAt <= ?1")
    void deleteApplicationArtifacts(Date date);

    @Modifying
    @Transactional
    @Query("delete from CdhServiceArtifact s where s.snapshot.createdAt <= ?1")
    void deleteCdhServiceArtifact(Date date);

    @Modifying
    @Transactional
    @Query("delete from CfServiceArtifact s where s.snapshot.createdAt <= ?1")
    void deleteCfServiceArtifact(Date date);
}
