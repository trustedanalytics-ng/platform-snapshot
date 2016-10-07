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

import org.springframework.context.annotation.Profile;
import org.trustedanalytics.platformsnapshot.client.TapOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
import org.trustedanalytics.platformsnapshot.client.entity.TapInfo;
import org.trustedanalytics.platformsnapshot.model.PlatformVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Supplier;

import rx.Observable;

@Service
@Profile("cloud")
public class PlatformVersionSupplier implements Supplier<Observable<PlatformVersion>> {

    private static final Logger LOG = LoggerFactory.getLogger(PlatformVersionSupplier.class);

    private final TapOperations tapOperations;
    private final CdhOperations cdhOperations;

    @Autowired
    public PlatformVersionSupplier(TapOperations tapOperations, CdhOperations cdhOperations) {
        this.tapOperations = Objects.requireNonNull(tapOperations,"tapOperations");
        this.cdhOperations = Objects.requireNonNull(cdhOperations, "cdhOperations");
    }

    @Override
    public Observable<PlatformVersion> get() {
        return Observable.zip(k8sVersion(), cdhVersion(), tapVersion(), PlatformVersion::new);
    }

    private Observable<String> k8sVersion() {
        return Observable.defer(tapOperations::getTapInfo)
            .map(TapInfo::getK8sVersion)
            .onErrorResumeNext(ex -> {
                LOG.error("Request for k8s version failed", ex);
                return Observable.just(null);
            });
    }

    private Observable<String> tapVersion() {
        return Observable.defer(tapOperations::getTapInfo)
                .map(TapInfo::getPlatformVersion)
                .onErrorResumeNext(ex -> {
                    LOG.error("Request for platform version failed", ex);
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
