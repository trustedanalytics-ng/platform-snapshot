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

import org.trustedanalytics.platformsnapshot.client.CfOperations;
import org.trustedanalytics.platformsnapshot.client.PlatformContext;
import org.trustedanalytics.platformsnapshot.client.PlatformContextOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.CfApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.CfServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.Scope;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import rx.Observable;
import rx.schedulers.Schedulers;

@Service
public class PlatformSnapshotScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformSnapshotScheduler.class);

    private final CfOperations cf;
    private final PlatformContextOperations ctx;
    private final PlatformSnapshotRepository repository;
    private final ScheduledExecutorService executor;
    private final CdhOperations cdhOperations;

    @Autowired
    public PlatformSnapshotScheduler(CfOperations cf,
                                     PlatformContextOperations ctx,
                                     PlatformSnapshotRepository repository,
                                     CdhOperations cdhOperations) {
        this.cf = Objects.requireNonNull(cf, CfOperations.class.getSimpleName());
        this.ctx = Objects.requireNonNull(ctx, PlatformContextOperations.class.getSimpleName());
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

            final PlatformContext context = platformContext();
            final UUID coreOrg = coreOrganization(context);
            final CdhCluster cdhCluster = cdhCluster();

            // @formatter:off
            Observable.zip(cfApplications(coreOrg).toList(), cfServices().toList(), cdhServices(cdhCluster.getName()).toList(), cfInfo(),
                (cfApps, cfServices, cdhServices, cfInfo) ->
                    new PlatformSnapshot(
                        new Date(),
                        context.getPlatformVersion(),
                        cfApps,
                        cdhCluster.getFullVersion(),
                        cfInfo.getApiVersion(),
                        cdhServices,
                        cfServices))
            .doOnNext(snapshot -> LOG.info("Persisting platform snapshot: {}", LocalDateTime.now()))
            .map(repository::save)
            .subscribe(snapshot -> LOG.info("Platform snapshot completed: {}", LocalDateTime.now()));
            // @formatter:on
        };
    }

    PlatformContext platformContext() {
        return Observable.defer(() -> Observable.just(ctx.getPlatformContext()))
            .onErrorResumeNext(Observable.just(new PlatformContext()))
            .doOnNext(context -> LOG.info("Platform context: {}", context))
            .toBlocking().single();
    }

    UUID coreOrganization(PlatformContext context) {
        return Observable.defer(() -> cf.getOrganization("name:" + context.getCoreOrganization()))
            .map(org -> org.getMetadata().getGuid())
            .switchIfEmpty(Observable.just(null))
            .onErrorResumeNext(Observable.just(null))
            .doOnNext(organization -> LOG.info("Core organization: {}", organization))
            .toBlocking().single();
    }

    CdhCluster cdhCluster() {
        return Observable.defer(() -> Observable.from(cdhOperations.getCdhClusters().getItems()))
            .first()
            .onErrorResumeNext(Observable.just(new CdhCluster()))
            .doOnNext(cluster -> LOG.info("CDH cluster: {}", cluster))
            .toBlocking().single();
    }

    Observable<CfApplicationArtifact> cfApplications(UUID coreOrg) {
        return cf.getSpaces()
            .flatMap(s -> Observable.defer(() -> cf.getApplications(s.getMetadata().getGuid()))
                .map(app -> new CfApplicationArtifact(app, s.getEntity().getOrganizationGuid(), Scope.resolve(coreOrg, s.getEntity().getOrganizationGuid()).toString()))
                .onErrorResumeNext(Observable.empty())
                .subscribeOn(Schedulers.io()));
    }

    Observable<CdhServiceArtifact> cdhServices(String clusterName) {
        return Observable.from(cdhOperations.getCdhServices(clusterName).getItems())
            .map(CdhServiceArtifact::new)
            .onErrorResumeNext(Observable.empty());
    }

    Observable<CfServiceArtifact> cfServices() {
        return cf.getServices()
            .map(CfServiceArtifact::new)
            .onErrorResumeNext(Observable.empty());
    }

    Observable<CfInfo> cfInfo() {
        return cf.getCfInfo()
            .onErrorResumeNext(Observable.just(new CfInfo()));
    }
}
