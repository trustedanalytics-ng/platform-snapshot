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
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
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
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        executor.scheduleAtFixedRate(snapshotTask(), 1, 8, TimeUnit.HOURS);
    }

    public void trigger() {
        LOG.info("Triggering platform snapshot: {}", LocalDateTime.now());
        executor.submit(snapshotTask());
    }

    private Runnable snapshotTask() {
        return () -> {
            LOG.info("Performing platform snapshot: {}", LocalDateTime.now());

            final PlatformContext context = ctx.getPlatformContext();
            LOG.info("Platform context: {}", context);
            final UUID coreOrg = cf.getOrganization("name:" + context.getCoreOrganization()).toBlocking().single().getMetadata().getGuid();
            final CdhCluster cdhCluster = cdhOperations.getCdhClusters().getItems().stream().findFirst().get();

            cf.getSpaces()
              .flatMap(s -> applications(s.getEntity().getOrganizationGuid(), s.getMetadata().getGuid(), coreOrg))
              .toList()
              .map(apps -> new PlatformSnapshot(new Date(),
                                                context.getPlatformVersion(),
                                                apps,
                                                cdhCluster.getFullVersion(),
                                                cfInfo().getApiVersion(),
                                                cdhServices(cdhCluster.getName()),
                                                cfServices()))
              .doOnNext(snapshot -> LOG.info("Persisting platform snapshot: {}", LocalDateTime.now()))
              .map(repository::save)
              .subscribe(snapshot -> LOG.info("Platform snapshot completed: {}", LocalDateTime.now()));
        };
    }

    private Observable<CfApplicationArtifact> applications(UUID organization, UUID space, UUID coreOrg) {
        return Observable.defer(() -> cf.getApplications(space))
                .map(app -> new CfApplicationArtifact(app, organization, Scope.resolve(coreOrg, organization)))
                .subscribeOn(Schedulers.io());
    }

    private Collection<CdhServiceArtifact> cdhServices(String clusterName) {
        return cdhOperations.getCdhServices(clusterName).getItems()
            .stream()
            .map(CdhServiceArtifact::new)
            .collect(Collectors.toList());
    }

    private Collection<CfServiceArtifact> cfServices() {
        return cf.getServices()
            .map(CfServiceArtifact::new)
            .toList()
            .toBlocking()
            .first();
    }

    private CfInfo cfInfo() {
        return cf.getCfInfo()
            .toBlocking()
            .first();
    }
}
