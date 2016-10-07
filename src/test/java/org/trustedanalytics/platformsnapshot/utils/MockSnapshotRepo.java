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
package org.trustedanalytics.platformsnapshot.utils;

import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;

import java.util.Collection;
import java.util.Date;

public class MockSnapshotRepo  implements PlatformSnapshotRepository{

    private PlatformSnapshot snapshot;

    public PlatformSnapshot getSavedSnapshot() {
        return snapshot;
    }


    @Override
    public Collection<PlatformSnapshot> findByDates(Date from, Date to) {
        return null;
    }

    @Override
    public PlatformSnapshot findTopByOrderByCreatedAtDesc() {
        return null;
    }

    @Override
    public void deletePlatformSnapshotsOlderThen(Date date) {

    }

    @Override
    public void deleteApplicationArtifacts(Date date) {

    }

    @Override
    public void deleteCdhServiceArtifact(Date date) {

    }

    @Override
    public void deleteCfServiceArtifact(Date date) {

    }

    @Override
    public <S extends PlatformSnapshot> S save(S entity) {
        System.out.println("Saving to mock repository: " + entity.toString());
        snapshot = entity;
        return entity;
    }

    @Override
    public <S extends PlatformSnapshot> Iterable<S> save(Iterable<S> entities) {
        return null;
    }

    @Override
    public PlatformSnapshot findOne(Long aLong) {
        return null;
    }

    @Override
    public boolean exists(Long aLong) {
        return false;
    }

    @Override
    public Iterable<PlatformSnapshot> findAll() {
        return null;
    }

    @Override
    public Iterable<PlatformSnapshot> findAll(Iterable<Long> longs) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void delete(Long aLong) {

    }

    @Override
    public void delete(PlatformSnapshot entity) {

    }

    @Override
    public void delete(Iterable<? extends PlatformSnapshot> entities) {

    }

    @Override
    public void deleteAll() {

    }
}
