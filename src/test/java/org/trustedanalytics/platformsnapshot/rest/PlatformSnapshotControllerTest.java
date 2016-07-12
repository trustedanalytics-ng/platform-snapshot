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

import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotConfiguration;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotTransactions;
import org.trustedanalytics.platformsnapshot.service.PlatformSnapshotScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class PlatformSnapshotControllerTest {

    @Mock
    PlatformSnapshotRepository platformSnapshotRepository;
    @Mock
    PlatformSnapshotScheduler platformSnapshotScheduler;
    @Mock
    PlatformSnapshotTransactions platformSnapshotTransactions;
    PlatformSnapshotController platformSnapshotController;

    @Before
    public void setUp() {
        platformSnapshotController = new PlatformSnapshotController(platformSnapshotRepository, platformSnapshotScheduler, platformSnapshotTransactions);
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

    private PlatformSnapshotConfiguration getPlatformSnapshotConfiguration() {
        return new PlatformSnapshotConfiguration(new ArrayList<>(Arrays.asList("CORE", "DEMO", "OTHER", "ALL")));
    }
}
