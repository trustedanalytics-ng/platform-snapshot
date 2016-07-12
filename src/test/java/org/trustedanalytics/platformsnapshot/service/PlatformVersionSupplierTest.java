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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.trustedanalytics.platformsnapshot.client.CfOperations;
import org.trustedanalytics.platformsnapshot.client.PlatformContext;
import org.trustedanalytics.platformsnapshot.client.PlatformContextOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhClusters;
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.model.PlatformVersion;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.UUID;

import rx.Observable;
import rx.observers.TestSubscriber;

@RunWith(Parameterized.class)
public class PlatformVersionSupplierTest {

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                {"all requests succeeded",
                    // given
                    cfVersion("1.0"), cdhVersion("1.1"), tapVersion("1.2"),
                    // then
                    new PlatformVersion("1.0", "1.1", "1.2")},
                {"cloud foundry request failed",
                    // given
                    cfVersion(), cdhVersion("1.1"), tapVersion("1.2"),
                    // then
                    new PlatformVersion(null, "1.1", "1.2")},
                {"cloudera request failed",
                    // given
                    cfVersion("1.0"), cdhVersion(), tapVersion("1.2"),
                    // then
                    new PlatformVersion("1.0", null, "1.2")},
                {"platform context request failed",
                    // given
                    cfVersion("1.0"), cdhVersion("1.1"), tapVersion(),
                    // then
                    new PlatformVersion("1.0", "1.1", null)},
                {"all requests failed",
                    // given
                    cfVersion(), cdhVersion(), tapVersion(),
                    // then
                    new PlatformVersion(null, null, null)}
            });
        // @formatter:on
    }

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public CfOperations cfOperations;

    @Parameterized.Parameter(2)
    public CdhOperations cdhOperations;

    @Parameterized.Parameter(3)
    public PlatformContextOperations ctxOperations;

    @Parameterized.Parameter(4)
    public PlatformVersion expected;

    @Test
    public void testPlatformVersion() {
        // given
        final PlatformVersionSupplier supplier =
            new PlatformVersionSupplier(cfOperations, cdhOperations, ctxOperations);

        // when
        final Observable<PlatformVersion> observable = supplier.get();

        // then
        final TestSubscriber<PlatformVersion> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        testSubscriber.assertValues(expected);
    }

    private static CfOperations cfVersion(String apiVersion) {
        CfOperations cfOperations = mock(CfOperations.class);
        when(cfOperations.getCfInfo()).thenReturn(Observable.just(new CfInfo("name", "build", 0, "description", apiVersion)));
        return cfOperations;
    }

    private static CfOperations cfVersion() {
        CfOperations cfOperations = mock(CfOperations.class);
        when(cfOperations.getCfInfo()).thenThrow(new IllegalStateException());
        return cfOperations;
    }

    private static CdhOperations cdhVersion(String cdhVersion) {
        CdhOperations cdhOperations = mock(CdhOperations.class);
        CdhClusters clusters = new CdhClusters();
        clusters.setItems(ImmutableSet.of(new CdhCluster("entityStatus", "name", "displayName", "version", cdhVersion, false, "clusterUrl", "hostUrl")));
        when(cdhOperations.getCdhClusters()).thenReturn(clusters);
        return cdhOperations;
    }

    private static CdhOperations cdhVersion() {
        CdhOperations cdhOperations = mock(CdhOperations.class);
        when(cdhOperations.getCdhClusters()).thenThrow(new IllegalStateException());
        return cdhOperations;
    }

    private static PlatformContextOperations tapVersion(String tapVersion) {
        PlatformContextOperations ctxOperations = mock(PlatformContextOperations.class);
        when(ctxOperations.getPlatformContext()).thenReturn(new PlatformContext("apiEndpoint", "cliVersion", "cliUrl", UUID
            .randomUUID()
            .toString(), tapVersion));
        return ctxOperations;
    }

    private static PlatformContextOperations tapVersion() {
        PlatformContextOperations ctxOperations = mock(PlatformContextOperations.class);
        when(ctxOperations.getPlatformContext()).thenThrow(new IllegalStateException());
        return ctxOperations;
    }

}
