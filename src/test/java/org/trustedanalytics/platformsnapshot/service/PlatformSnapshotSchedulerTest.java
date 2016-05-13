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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.platformsnapshot.client.CfOperations;
import org.trustedanalytics.platformsnapshot.client.PlatformContext;
import org.trustedanalytics.platformsnapshot.client.PlatformContextOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhClusters;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhService;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhServices;
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.client.entity.CfMetadata;
import org.trustedanalytics.platformsnapshot.client.entity.CfOrganization;
import org.trustedanalytics.platformsnapshot.client.entity.CfService;
import org.trustedanalytics.platformsnapshot.client.entity.CfServiceEntity;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.CfApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.CfServiceArtifact;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class PlatformSnapshotSchedulerTest {

    PlatformSnapshotScheduler platformSnapshotScheduler;

    @Mock
    CfOperations cfOperations;

    @Mock
    PlatformContextOperations platformContextOperations;

    @Mock
    PlatformSnapshotRepository platformSnapshotRepository;

    @Mock
    CdhOperations cdhOperations;

    @Before
    public void setUp() {
        platformSnapshotScheduler = new PlatformSnapshotScheduler(cfOperations, platformContextOperations, platformSnapshotRepository, cdhOperations);
    }

    @Test
    public void testPlatformContext() {
        // given
        PlatformContext expectedPlatformContext = new PlatformContext();
        expectedPlatformContext.setCoreOrganization("coreOrg");

        // when
        when(platformContextOperations.getPlatformContext()).thenReturn(expectedPlatformContext);
        PlatformContext actualPlatformContext = platformSnapshotScheduler.platformContext();

        // then
        assertEquals(expectedPlatformContext, actualPlatformContext);
    }

    @Test
    public void testPlatformContextOnError() {
        // given
        PlatformContext expectedPlatformContext = new PlatformContext();

        // when
        when(platformContextOperations.getPlatformContext()).thenThrow(new IllegalStateException());
        PlatformContext actualPlatformContext = platformSnapshotScheduler.platformContext();

        // then
        assertEquals(expectedPlatformContext, actualPlatformContext);
    }

    @Test
    public void testCoreOrganization() {

        // given
        UUID expectedUuid = UUID.randomUUID();
        CfOrganization cfOrganization = getOrganization(expectedUuid);

        // when
        when(cfOperations.getOrganization(any())).thenReturn(Observable.just(cfOrganization));
        UUID actualUuid = platformSnapshotScheduler.coreOrganization(new PlatformContext());

        // then
        assertEquals(expectedUuid, actualUuid);
    }

    @Test
    public void testCoreOrganizationEmptyCoreOrg() {

        // given
        CfOrganization cfOrganization = getOrganization(null);

        // when
        when(cfOperations.getOrganization(any())).thenReturn(Observable.just(cfOrganization));
        UUID actualUuid = platformSnapshotScheduler.coreOrganization(new PlatformContext());

        // then
        assertEquals(null, actualUuid);
    }

    @Test
    public void testCoreOrganizationEmptyOnError() {

        // when
        when(cfOperations.getOrganization(any())).thenThrow(new IllegalStateException());
        UUID actualUuid = platformSnapshotScheduler.coreOrganization(new PlatformContext());

        // then
        assertEquals(null, actualUuid);
    }

    @Test
    public void testCdhCluster() {

        // when
        when(cdhOperations.getCdhClusters()).thenReturn(getCdhClusters(getCdhCluster()));
        CdhCluster actualCdhCluster = platformSnapshotScheduler.cdhCluster();

        // then
        assertEquals(getCdhCluster(), actualCdhCluster);
    }

    @Test
    public void testCdhClusterNoCluster() {

        // when
        when(cdhOperations.getCdhClusters()).thenReturn(getCdhClusters(null));
        CdhCluster actualCdhCluster = platformSnapshotScheduler.cdhCluster();

        // then
        assertEquals(null, actualCdhCluster);
    }

    @Test
    public void testCdhClusterNoClusters() {

        // when
        when(cdhOperations.getCdhClusters()).thenReturn(null);
        CdhCluster actualCdhCluster = platformSnapshotScheduler.cdhCluster();

        // then
        assertEquals(new CdhCluster(), actualCdhCluster);
    }

    @Test
    public void testCdhClusterOnError() {

        // when
        when(cdhOperations.getCdhClusters()).thenThrow(new IllegalStateException());
        CdhCluster actualCdhCluster = platformSnapshotScheduler.cdhCluster();
        CdhCluster expectedCdhCluster = new CdhCluster();

        // then
        assertEquals(expectedCdhCluster, actualCdhCluster);
    }

    @Test
    public void testCfApplicationsOnEmpty() throws IOException {

        // when
        when(cfOperations.getSpaces()).thenReturn(Observable.empty());
        when(cfOperations.getApplications(any(UUID.class))).thenReturn(Observable.empty());

        final TestSubscriber<CfApplicationArtifact> testSubscriber = new TestSubscriber<>();
        platformSnapshotScheduler.cfApplications(UUID.randomUUID()).subscribe(testSubscriber);

        // then
        testSubscriber.assertNoValues();
    }

    @Test
    public void testCdhServices() {

        // given
        final String clusterName = "clusterName";

        // when
        when(cdhOperations.getCdhServices(clusterName)).thenReturn(getCdhServices());
        CdhServiceArtifact actualCdhServices = platformSnapshotScheduler.cdhServices(clusterName).toBlocking().first();

        // then
        assertEquals(getCdhServices().getItems().stream().findFirst().get().getName(), actualCdhServices.getName());
    }

    @Test
    public void testCfServices() {

        // given
        CfService expectedCfService = getCfService();

        // when
        final TestSubscriber<CfServiceArtifact> testSubscriber = new TestSubscriber<>();
        when(cfOperations.getServices()).thenReturn(Observable.just(expectedCfService));
        platformSnapshotScheduler.cfServices().subscribe(testSubscriber);

        // then
        testSubscriber.assertValue(new CfServiceArtifact(expectedCfService));
    }

    @Test
    public void testCfServicesOnError() {

        // when
        when(cfOperations.getServices()).thenReturn(Observable.just(null));

        final TestSubscriber<CfServiceArtifact> testSubscriber = new TestSubscriber<>();
        platformSnapshotScheduler.cfServices().subscribe(testSubscriber);

        // then
        testSubscriber.assertNoValues();
    }

    @Test
    public void testCfInfo() {

        // when
        when(cfOperations.getCfInfo()).thenReturn(Observable.just(getCfInfo()));

        final TestSubscriber<CfInfo> testSubscriber = new TestSubscriber<>();
        platformSnapshotScheduler.cfInfo().subscribe(testSubscriber);

        // then
        testSubscriber.assertValues(getCfInfo());
    }

    @Test
    public void testCfInfoOnEmpty() {

        // when
        when(cfOperations.getCfInfo()).thenReturn(Observable.empty());

        final TestSubscriber<CfInfo> testSubscriber = new TestSubscriber<>();
        platformSnapshotScheduler.cfInfo().subscribe(testSubscriber);

        // then
        testSubscriber.assertNoValues();
    }

    private CfInfo getCfInfo() {
        CfInfo cfInfo = new CfInfo();
        cfInfo.setVersion(1);
        return cfInfo;
    }

    private CfService getCfService() {
        CfService cfService = new CfService();
        cfService.setMetadata(new CfMetadata());
        cfService.setEntity(new CfServiceEntity());
        cfService.getMetadata().setCreatedAt(LocalDateTime.of(2016, 4, 15, 15, 45, 0));
        cfService.getMetadata().setGuid(UUID.randomUUID());
        return cfService;
    }

    private CdhServices getCdhServices() {
        CdhServices cdhServices = new CdhServices();
        CdhService cdhService = new CdhService();
        cdhService.setName("cdhService");
        cdhServices.setItems(new ArrayList<>());
        cdhServices.getItems().add(cdhService);
        return cdhServices;
    }

    private CdhClusters getCdhClusters(CdhCluster cdhCluster) {
        CdhClusters cdhClusters = new CdhClusters();
        Collection<CdhCluster> items = new ArrayList<>();
        items.add(cdhCluster);
        cdhClusters.setItems(items);
        return cdhClusters;
    }

    private CdhCluster getCdhCluster() {
        CdhCluster item = new CdhCluster();
        item.setClusterUrl("url");
        return item;
    }

    private CfOrganization getOrganization(UUID uuid) {
        CfOrganization cfOrganization = new CfOrganization();
        cfOrganization.setMetadata(new CfMetadata());
        cfOrganization.getMetadata().setGuid(uuid);
        return cfOrganization;
    }
}
