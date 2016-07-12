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

import org.trustedanalytics.platformsnapshot.client.CfOperations;
import org.trustedanalytics.platformsnapshot.client.PlatformContext;
import org.trustedanalytics.platformsnapshot.client.PlatformContextOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.model.PlatformVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Supplier;

import rx.Observable;

@Service
public class PlatformVersionSupplier implements Supplier<Observable<PlatformVersion>> {

    private static final Logger LOG = LoggerFactory.getLogger(PlatformVersionSupplier.class);

    private final CfOperations cfOperations;
    private final CdhOperations cdhOperations;
    private final PlatformContextOperations ctxOperations;

    @Autowired
    public PlatformVersionSupplier(CfOperations cfOperations, CdhOperations cdhOperations,
        PlatformContextOperations ctxOperations) {
        this.cfOperations = Objects.requireNonNull(cfOperations, "cfOperations");
        this.cdhOperations = Objects.requireNonNull(cdhOperations, "cdhOperations");
        this.ctxOperations = Objects.requireNonNull(ctxOperations, "ctxOperations");
    }

    @Override
    public Observable<PlatformVersion> get() {
        return Observable.zip(cfVersion(), cdhVersion(), tapVersion(), PlatformVersion::new);
    }

    private Observable<String> cfVersion() {
        return Observable.defer(cfOperations::getCfInfo)
            .map(CfInfo::getApiVersion)
            .onErrorResumeNext(ex -> {
                LOG.error("Request for cloud foundry version failed", ex);
                return Observable.just(null);
            });
    }

    private Observable<String> tapVersion() {
        return Observable.defer(() -> Observable.just(ctxOperations.getPlatformContext()))
            .map(PlatformContext::getPlatformVersion)
            .onErrorResumeNext(ex -> {
                LOG.error("Request for trusted analytics platform version failed", ex);
                return Observable.just(null);
            });
    }

    private Observable<String> cdhVersion() {
        return Observable.defer(() -> Observable.from(cdhOperations.getCdhClusters().getItems()))
            .limit(1)
            .map(CdhCluster::getFullVersion)
            .onErrorResumeNext(ex -> {
                LOG.error("Request for cloudera version failed", ex);
                return Observable.just(null);
            });
    }
}
