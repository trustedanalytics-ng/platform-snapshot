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
package org.trustedanalytics.platformsnapshot.service;

import org.springframework.context.annotation.Profile;
import org.trustedanalytics.platformsnapshot.client.TapOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhService;
import org.trustedanalytics.platformsnapshot.client.entity.TapInfo;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.TapApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.TapServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import rx.Observable;

@Service
@Profile("cloud")
public class PlatformSnapshotScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformSnapshotScheduler.class);

    private final TapOperations tap;

    private final PlatformSnapshotRepository repository;
    private final ScheduledExecutorService executor;
    private final CdhOperations cdhOperations;

    @Autowired
    public PlatformSnapshotScheduler(TapOperations tap,
                                     PlatformSnapshotRepository repository,
                                     CdhOperations cdhOperations) {
        this.tap = Objects.requireNonNull(tap, TapOperations.class.getSimpleName());
        this.repository = Objects.requireNonNull(repository, PlatformSnapshotRepository.class.getSimpleName());
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.cdhOperations = cdhOperations;
    }

    @PostConstruct
    private void schedule() {
        LOG.info("Scheduling platform snapshot: {}", LocalDateTime.now());
        executor.scheduleAtFixedRate(snapshotTask(), 1, 24, TimeUnit.HOURS);
    }

    public void trigger() {
        LOG.info("Triggering platform snapshot: {}", LocalDateTime.now());
        executor.submit(snapshotTask());
    }

    private Runnable snapshotTask() {
        return () -> {
            LOG.info("Performing platform snapshot: {}", LocalDateTime.now());
            final CdhCluster cdhCluster = cdhCluster();
            LOG.info("Cdh cluster received {}", cdhCluster);
            // @formatter:off
            Observable.zip(tapApplications().toList(), tapServices().toList(), cdhServices(cdhCluster.getName()).toList(), tapInfo(),
                (tapApps, tapServices, cdhServices, tapInfo) ->
                    new PlatformSnapshot(
                        new Date(),
                        tapInfo.getPlatformVersion(),
                        tapApps,
                        cdhCluster.getFullVersion(),
                        tapInfo.getK8sVersion(),
                        cdhServices,
                        tapServices))
            .doOnNext(snapshot -> LOG.info("Persisting platform snapshot: {}", LocalDateTime.now()))
            .map(repository::save)
            .subscribe(snapshot -> LOG.info("Platform snapshot completed: {}", LocalDateTime.now()));
            // @formatter:on
        };
    }

    CdhCluster cdhCluster() {
        return Observable.defer(() -> Observable.from(cdhOperations.getCdhClusters().getItems()))
            .first()
                .map(cluster -> {
                    try {
                        return cluster;
                    } catch (Exception e) {
                        LOG.error("Error while fetching CdhCluster {}", e);
                        throw e;
                    }
                })
            .onErrorResumeNext(Observable.just(new CdhCluster()))
            .doOnNext(cluster -> LOG.info("CDH cluster: {}", cluster))
            .toBlocking().single();
    }

    Observable<TapApplicationArtifact> tapApplications() {
        return Observable.defer(() -> tap.getApplications()
                .map(app -> {
                    try {
                        return new TapApplicationArtifact(app);
                    } catch (Exception e) {
                        LOG.error("Error while creating TapApplicationArtifact {}", e);
                        throw e;
                    }
                })
                .onErrorResumeNext(Observable.just(new TapApplicationArtifact()))
                .doOnNext(artifact -> LOG.info("Application artifact: {}", artifact)));

    }

    Observable<CdhServiceArtifact> cdhServices(String clusterName) {
        return Observable.defer(
                () -> Observable.from(
                        cdhOperations.getCdhServices(clusterName).getItems())
                       .map(cdhArtifact -> {
                        try {
                           return new CdhServiceArtifact(cdhArtifact);
                        } catch (Exception e) {
                        LOG.error("Error while creating cdh artifact, {}", e);
                        throw e;
                    }
                }))
                .onErrorResumeNext(ex -> {
                    LOG.error("Error {}", ex);
                    return Observable.just(new CdhServiceArtifact(new CdhService())); }
                )
                .switchIfEmpty(
                        Observable.just(new CdhServiceArtifact(new CdhService()))

        );

    }

    Observable<TapServiceArtifact> tapServices() {
        return Observable.defer(() -> tap.getServices()
            .map(service -> {
                        try {
                            return new TapServiceArtifact(service);
                        } catch (Exception e) {
                            LOG.error("Error while creating TapServiceArtifact {}", e);
                            throw e;
                        }
                    }
            )
            .onErrorResumeNext(Observable.just(new TapServiceArtifact()))
                .doOnNext(artifact -> LOG.info("Service artifact: {}", artifact)));

    }

    Observable<TapInfo> tapInfo() {
        return Observable.defer(() -> tap.getTapInfo()
            .onErrorResumeNext(Observable.just(new TapInfo())));
    }

}
