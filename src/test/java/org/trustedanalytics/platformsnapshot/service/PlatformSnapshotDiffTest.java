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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.platformsnapshot.model.FlattenPlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.PartitionedPlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PlatformSnapshotDiffTest {

    private PlatformSnapshotDiffService service;
    private ObjectMapper mapper;

    @Mock
    PlatformSnapshotRepository repository;

    @Before
    public void setUp() {
        service = new PlatformSnapshotDiffService(repository);
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy());
    }

    @Test
    public void testDiffTwoExistingSnapshots() throws IOException {
        // given
        PlatformSnapshot before = readObjectFromFile(mapper, "snapshot_before.json", PlatformSnapshot.class);
        PlatformSnapshot after = readObjectFromFile(mapper, "snapshot_after.json", PlatformSnapshot.class);
        PlatformSnapshotDiff expectedDiff = readObjectFromFile(mapper, "diff.json", FlattenPlatformSnapshotDiff.class);
        mockRepository(after, before);

        // when
        PlatformSnapshotDiff actualDiff = service.diff(before.getId(), after.getId());

        // then
        assertEquals(mapper.writeValueAsString(expectedDiff), mapper.writeValueAsString(actualDiff));
    }

    @Test
    public void testDiffsAggregation() throws IOException {
        // given
        PlatformSnapshot before = readObjectFromFile(mapper, "snapshot_before.json", PlatformSnapshot.class);
        PlatformSnapshot after = readObjectFromFile(mapper, "snapshot_after.json", PlatformSnapshot.class);
        PlatformSnapshotDiff expectedDiff = readObjectFromFile(mapper, "aggregated_diff.json", PartitionedPlatformSnapshotDiff.class);
        mockRepository(after, before);

        // when
        PlatformSnapshotDiff actualDiff = service.diffByType(before.getId(), after.getId());

        // then
        assertEquals(mapper.writeValueAsString(expectedDiff), mapper.writeValueAsString(actualDiff));
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testDiffWithNotExistingSnapshots() throws IOException {
        PlatformSnapshot before = readObjectFromFile(mapper, "snapshot_before.json", PlatformSnapshot.class);
        PlatformSnapshot after = readObjectFromFile(mapper, "snapshot_after.json", PlatformSnapshot.class);
        when(repository.findOne(before.getId())).thenReturn(before);

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format("Snapshot with id %s does not exist", after.getId()));
        service.diff(before.getId(), after.getId());
    }

    private void mockRepository(PlatformSnapshot after, PlatformSnapshot before) {
        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);
    }

    private <T> T readObjectFromFile(ObjectMapper mapper, String resource, Class<T> clazz) throws IOException {
        return mapper.readValue(getClass().getClassLoader().getResource(resource), clazz);
    }

}