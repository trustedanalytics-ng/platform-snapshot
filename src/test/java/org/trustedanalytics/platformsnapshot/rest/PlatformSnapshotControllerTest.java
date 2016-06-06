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
package org.trustedanalytics.platformsnapshot.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.CfApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.CfServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotConfiguration;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import org.trustedanalytics.platformsnapshot.service.PlatformSnapshotScheduler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class PlatformSnapshotControllerTest {

    @Mock
    PlatformSnapshotRepository platformSnapshotRepository;
    @Mock
    PlatformSnapshotScheduler platformSnapshotScheduler;
    PlatformSnapshotController platformSnapshotController;

    @Before
    public void setUp() {
        platformSnapshotController = new PlatformSnapshotController(platformSnapshotRepository, platformSnapshotScheduler);
    }

    @Test
    public void testGetPlatformSnapshot() {
        LocalDateTime from = LocalDateTime.of(2016, 5, 11, 9, 45, 0);
        LocalDateTime to = LocalDateTime.of(2016, 5, 10, 9, 45, 0);

        platformSnapshotController.getPlatformSnapshot(Optional.of(from), Optional.of(to), Optional.empty());
        verify(platformSnapshotRepository).findByDates(Date.from(from.toInstant(ZoneOffset.UTC)), Date.from(to.toInstant(ZoneOffset.UTC)));
    }

    @Test
    public void testGetPlatformSnapshotWithDefaults() {
        final int defaultDaysRange = 7;
        final int minutesOffset = 1;

        ArgumentCaptor<Date> from = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> to = ArgumentCaptor.forClass(Date.class);

        Date expectedFromMinusMinute = Date.from(LocalDateTime.now().minusDays(defaultDaysRange).minusMinutes(minutesOffset).toInstant(ZoneOffset.UTC));
        Date expectedToMinusMinute = Date.from(LocalDateTime.now().minusMinutes(minutesOffset).toInstant(ZoneOffset.UTC));
        Date expectedFromPlusMinute = Date.from(LocalDateTime.now().minusDays(defaultDaysRange).plusMinutes(minutesOffset).toInstant(ZoneOffset.UTC));
        Date expectedToPlusMinute = Date.from(LocalDateTime.now().plusMinutes(minutesOffset).toInstant(ZoneOffset.UTC));

        platformSnapshotController.getPlatformSnapshot(Optional.empty(), Optional.empty(), Optional.empty());
        verify(platformSnapshotRepository).findByDates(from.capture(), to.capture());

        assertTrue(expectedFromMinusMinute.before(from.getValue()));
        assertTrue(expectedFromPlusMinute.after(from.getValue()));
        assertTrue(expectedToMinusMinute.before(to.getValue()));
        assertTrue(expectedToPlusMinute.after(to.getValue()));
    }

    @Test
    public void testGetPlatformSnapshotSummary() {

        PlatformSnapshot sourcePlatformSnapshot = getPlatformSnapshot();
        assertEquals(2, sourcePlatformSnapshot.getApplications().size());
        assertEquals(1, sourcePlatformSnapshot.getCdhServices().size());
        assertEquals(1, sourcePlatformSnapshot.getCfServices().size());

        when(platformSnapshotRepository.findTopByOrderByCreatedAtDesc()).thenReturn(sourcePlatformSnapshot);
        PlatformSnapshot actualPlatformSnapshot = platformSnapshotController.getPlatformSnapshotSummary();

        assertEquals(0, actualPlatformSnapshot.getApplications().size());
        assertEquals(0, actualPlatformSnapshot.getCdhServices().size());
        assertEquals(0, actualPlatformSnapshot.getCfServices().size());
        assertEquals("cfVersion", actualPlatformSnapshot.getCfVersion());
        assertEquals("cdhVersion", actualPlatformSnapshot.getCdhVersion());
        assertEquals("platformVersion", actualPlatformSnapshot.getPlatformVersion());
    }

    @Test
    public void testGetPlatformSnapshotById() {
        platformSnapshotController.getPlatformSnapshot(1L);
        verify(platformSnapshotRepository).findOne(1L);
    }

    @Test
    public void testTriggerPlatformSnapshot() {
        platformSnapshotController.triggerPlatformSnapshot();
        verify(platformSnapshotScheduler).trigger();
    }

    @Test
    public void testGetPlatformSnapshotConfiguration() {
        PlatformSnapshotConfiguration actualConfiguration = platformSnapshotController.getPlatformConfiguration();
        PlatformSnapshotConfiguration expectedConfiguration = getPlatformSnapshotConfiguration();
        assertEquals(expectedConfiguration, actualConfiguration);
    }

    private PlatformSnapshot getPlatformSnapshot() {
        PlatformSnapshot platformSnapshot = new PlatformSnapshot();
        Collection<CfApplicationArtifact> cfApplicationArtifacts = new ArrayList<>();

        CfApplicationArtifact cfApplicationArtifact1 = new CfApplicationArtifact();
        cfApplicationArtifact1.setGuid(UUID.randomUUID().toString());
        CfApplicationArtifact cfApplicationArtifact2 = new CfApplicationArtifact();
        cfApplicationArtifact2.setGuid(UUID.randomUUID().toString());

        cfApplicationArtifacts.add(cfApplicationArtifact1);
        cfApplicationArtifacts.add(cfApplicationArtifact2);

        Collection<CfServiceArtifact> cfServices = new ArrayList<>();
        CfServiceArtifact cfService = new CfServiceArtifact();
        cfServices.add(cfService);

        Collection<CdhServiceArtifact> cdhServiceArtifacts = new ArrayList<>();
        CdhServiceArtifact cdhServiceArtifact = new CdhServiceArtifact();
        cdhServiceArtifacts.add(cdhServiceArtifact);

        platformSnapshot.setApplications(cfApplicationArtifacts);
        platformSnapshot.setCfServices(cfServices);
        platformSnapshot.setCdhServices(cdhServiceArtifacts);
        platformSnapshot.setCdhVersion("cdhVersion");
        platformSnapshot.setCfVersion("cfVersion");
        platformSnapshot.setPlatformVersion("platformVersion");
        return platformSnapshot;
    }

    private PlatformSnapshotConfiguration getPlatformSnapshotConfiguration() {
        return new PlatformSnapshotConfiguration(new ArrayList<>(Arrays.asList("CORE", "DEMO", "OTHER", "ALL")));
    }
}
