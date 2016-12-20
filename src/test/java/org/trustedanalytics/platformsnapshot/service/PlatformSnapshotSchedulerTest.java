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
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.trustedanalytics.platformsnapshot.client.TapOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhCluster;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhClusters;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhService;
import org.trustedanalytics.platformsnapshot.client.cdh.entity.CdhServices;
import org.trustedanalytics.platformsnapshot.client.entity.*;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.TapServiceArtifact;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(MockitoJUnitRunner.class)
public class PlatformSnapshotSchedulerTest {

    PlatformSnapshotScheduler platformSnapshotScheduler;

    @Mock
    TapOperations tapOperations;

    @Mock
    PlatformSnapshotRepository platformSnapshotRepository;

    @Mock
    CdhOperations cdhOperations;

    @Before
    public void setUp() {
        platformSnapshotScheduler = new PlatformSnapshotScheduler(tapOperations, platformSnapshotRepository, cdhOperations);
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
        TapService expectedTapService = getCfService();

        // when
        final TestSubscriber<TapServiceArtifact> testSubscriber = new TestSubscriber<>();
        when(tapOperations.getServices()).thenReturn(Observable.just(expectedTapService));
        platformSnapshotScheduler.tapServices().subscribe(testSubscriber);

        // then
        testSubscriber.assertValue(new TapServiceArtifact(expectedTapService));

    }

    @Test
    public void testCfServicesOnError() {

        // when
        when(tapOperations.getServices()).thenReturn(Observable.just(null));

        final TestSubscriber<TapServiceArtifact> testSubscriber = new TestSubscriber<>();
        platformSnapshotScheduler.tapServices().subscribe(testSubscriber);

        // then
        testSubscriber.assertValue(new TapServiceArtifact());
    }

    @Test
    public void testCfInfo() {

        /* TODO
        // when
        when(tapOperations.getCfInfo()).thenReturn(Observable.just(getCfInfo()));

        final TestSubscriber<CfInfo> testSubscriber = new TestSubscriber<>();
        platformSnapshotScheduler.cfInfo().subscribe(testSubscriber);

        // then
        testSubscriber.assertValues(getCfInfo());
        */
    }

    @Test
    public void testCfInfoOnEmpty() {
/* TODO
        // when
        when(tapOperations.getCfInfo()).thenReturn(Observable.empty());

        final TestSubscriber<CfInfo> testSubscriber = new TestSubscriber<>();
        platformSnapshotScheduler.cfInfo().subscribe(testSubscriber);

        // then
        testSubscriber.assertNoValues();
        */
    }

    private TapInfo getCfInfo() {
        TapInfo tapInfo = new TapInfo();
        tapInfo.setPlatformVersion("1");
        return tapInfo;
    }

    private TapService getCfService() {
        TapService tapService = new TapService();
        tapService.setMetadata(new TapMetadata());
        tapService.setEntity(new TapServiceEntity());
        tapService.getMetadata().setCreatedAt(LocalDateTime.of(2016, 4, 15, 15, 45, 0));
        tapService.getMetadata().setGuid("someID");
        return tapService;
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

    private TapOrganization getOrganization(String orgid) {
        TapOrganization tapOrganization = new TapOrganization();
        tapOrganization.setMetadata(new TapMetadata());
        tapOrganization.getMetadata().setGuid(orgid);
        return tapOrganization;
    }
}
