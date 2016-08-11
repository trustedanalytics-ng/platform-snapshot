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
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.platformsnapshot.model.CdhServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.CfApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.CfServiceArtifact;
import org.trustedanalytics.platformsnapshot.model.FlattenPlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.PartitionedPlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import static de.danielbechler.diff.node.DiffNode.State;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PlatformSnapshotDiffTest {

    private PlatformSnapshotDiffService service;
    private ObjectMapper mapper;
    private Date date;

    @Mock
    PlatformSnapshotRepository repository;

    @Before
    public void setUp() {
        service = new PlatformSnapshotDiffService(repository);
        mapper = new ObjectMapper().setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy());
        date = new Date();
    }

    @Test
    public void testDiffTwoExistingSnapshots() throws IOException {
        // given
        final PlatformSnapshot before = readObjectFromFile(mapper, "snapshot_before.json", PlatformSnapshot.class);
        final PlatformSnapshot after = readObjectFromFile(mapper, "snapshot_after.json", PlatformSnapshot.class);
        final PlatformSnapshotDiff expectedDiff = readObjectFromFile(mapper, "diff.json", FlattenPlatformSnapshotDiff.class);
        mockRepository(after, before);

        // when
        final PlatformSnapshotDiff actualDiff = service.diff(before.getId(), after.getId());

        // then
        assertEquals(mapper.writeValueAsString(expectedDiff), mapper.writeValueAsString(actualDiff));
    }

    @Test
    public void testDiffsAggregation() throws IOException {
        // given
        final PlatformSnapshot before = readObjectFromFile(mapper, "snapshot_before.json", PlatformSnapshot.class);
        final PlatformSnapshot after = readObjectFromFile(mapper, "snapshot_after.json", PlatformSnapshot.class);
        final PlatformSnapshotDiff expectedDiff = readObjectFromFile(mapper, "aggregated_diff.json", PartitionedPlatformSnapshotDiff.class);
        mockRepository(after, before);

        // when
        final PlatformSnapshotDiff actualDiff = service.diffByType(before.getId(), after.getId());

        // then
        assertEquals(mapper.writeValueAsString(expectedDiff), mapper.writeValueAsString(actualDiff));
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testDiffWithNotExistingSnapshots() throws IOException {
        final PlatformSnapshot before = readObjectFromFile(mapper, "snapshot_before.json", PlatformSnapshot.class);
        final PlatformSnapshot after = readObjectFromFile(mapper, "snapshot_after.json", PlatformSnapshot.class);
        when(repository.findOne(before.getId())).thenReturn(before);

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format("Snapshot with id %s does not exist", after.getId()));
        service.diff(before.getId(), after.getId());
    }

    @Test
    public void testDeletedSingleCloudFoundryComponent() {
        //given
        final String guid = "176eb6b8-c2a9-49f8-8c67-6c4883ca01bf";
        final CfServiceArtifact artifactBefore = createCfServiceArtifact("yarn", Optional.of(guid), Optional.empty(), Optional.empty());
        final PlatformSnapshot before = createPlatformSnapshot(1L, date, ImmutableList.of(artifactBefore), ImmutableList.of(), ImmutableList.of());

        final CfServiceArtifact artifactAfter = createCfServiceArtifact("yarn", Optional.empty(), Optional.of(date), Optional.of("description of service"));
        final PlatformSnapshot after = createPlatformSnapshot(2L, date, ImmutableList.of(artifactAfter), ImmutableList.of(), ImmutableList.of());

        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);

        //when
        final FlattenPlatformSnapshotDiff diff = (FlattenPlatformSnapshotDiff) service.diff(before.getId(), after.getId());

        //then
        assertTrue(diff.getComponents().stream().allMatch(c -> State.REMOVED.toString().equals(c.getOperation())));
    }

    @Test
    public void testDeletedSingleHadoopComponent() {
        //given
        final CdhServiceArtifact artifactBefore = createCdhServiceArtifact(Optional.of("hue"), Optional.empty());
        final PlatformSnapshot before = createPlatformSnapshot(1L, date, ImmutableList.of(), ImmutableList.of(artifactBefore), ImmutableList.of());

        final CdhServiceArtifact artifactAfter = createCdhServiceArtifact(Optional.empty(), Optional.of("started"));
        final PlatformSnapshot after = createPlatformSnapshot(2L, date, ImmutableList.of(), ImmutableList.of(artifactAfter), ImmutableList.of());

        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);

        //when
        final FlattenPlatformSnapshotDiff diff = (FlattenPlatformSnapshotDiff) service.diff(before.getId(), after.getId());

        //then
        assertTrue(diff.getComponents().stream().allMatch(c -> State.REMOVED.toString().equals(c.getOperation())));
    }
    
    @Test
    public void testAddedSingleCloudFoundryComponent() {
        //given
        final String guid = "176eb6b8-c2a9-49f8-8c67-6c4883ca01bf";
        final CfServiceArtifact artifactBefore = createCfServiceArtifact("yarn", Optional.empty(), Optional.of(date), Optional.of("description of service"));
        final PlatformSnapshot before = createPlatformSnapshot(1L, date, ImmutableList.of(artifactBefore), ImmutableList.of(), ImmutableList.of());

        final CfServiceArtifact artifactAfter = createCfServiceArtifact("yarn", Optional.of(guid), Optional.empty(), Optional.empty());
        final PlatformSnapshot after = createPlatformSnapshot(2L, date, ImmutableList.of(artifactAfter), ImmutableList.of(), ImmutableList.of());

        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);

        //when
        final FlattenPlatformSnapshotDiff diff = (FlattenPlatformSnapshotDiff) service.diff(before.getId(), after.getId());

        //then
        assertTrue(diff.getComponents().stream().allMatch(c -> State.ADDED.toString().equals(c.getOperation())));
    }

    @Test
    public void testAddedSingleHadoopComponent() {
        //given
        final CdhServiceArtifact artifactBefore = createCdhServiceArtifact(Optional.empty(), Optional.of("started"));
        final PlatformSnapshot before = createPlatformSnapshot(1L, date, ImmutableList.of(), ImmutableList.of(artifactBefore), ImmutableList.of());

        final CdhServiceArtifact artifactAfter = createCdhServiceArtifact(Optional.of("hue"), Optional.empty());
        final PlatformSnapshot after = createPlatformSnapshot(2L, date, ImmutableList.of(), ImmutableList.of(artifactAfter), ImmutableList.of());

        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);

        //when
        final FlattenPlatformSnapshotDiff diff = (FlattenPlatformSnapshotDiff) service.diff(before.getId(), after.getId());

        //then
        assertTrue(diff.getComponents().stream().allMatch(c -> State.ADDED.toString().equals(c.getOperation())));
    }

    @Test
    public void testChangedSingleCloudFoundryComponent() {
        //given
        final String guid = "176eb6b8-c2a9-49f8-8c67-6c4883ca01bf";
        final CfServiceArtifact artifactBefore = createCfServiceArtifact("yarn", Optional.of(guid), Optional.of(date), Optional.of("description of service"));
        final PlatformSnapshot before = createPlatformSnapshot(1L, date, ImmutableList.of(artifactBefore), ImmutableList.of(), ImmutableList.of());

        final CfServiceArtifact artifactAfter = createCfServiceArtifact("yarn", Optional.of(guid), Optional.of(DateUtils.addHours(date, 1)),
                Optional.of("new description of service"));
        final PlatformSnapshot after = createPlatformSnapshot(2L, date, ImmutableList.of(artifactAfter), ImmutableList.of(), ImmutableList.of());

        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);

        //when
        final FlattenPlatformSnapshotDiff diff = (FlattenPlatformSnapshotDiff) service.diff(before.getId(), after.getId());

        //then
        assertTrue(diff.getComponents().stream().allMatch(c -> State.CHANGED.toString().equals(c.getOperation())));
    }

    @Test
    public void testChangedSingleHadoopComponent() {
        //given
        final CdhServiceArtifact artifactBefore = createCdhServiceArtifact(Optional.of("hue"), Optional.of("started"));
        final PlatformSnapshot before = createPlatformSnapshot(1L, date, ImmutableList.of(), ImmutableList.of(artifactBefore), ImmutableList.of());

        final CdhServiceArtifact artifactAfter = createCdhServiceArtifact(Optional.of("hue"), Optional.of("stopped"));
        final PlatformSnapshot after = createPlatformSnapshot(2L, date, ImmutableList.of(), ImmutableList.of(artifactAfter), ImmutableList.of());

        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);

        //when
        final FlattenPlatformSnapshotDiff diff = (FlattenPlatformSnapshotDiff) service.diff(before.getId(), after.getId());

        //then
        assertTrue(diff.getComponents().stream().allMatch(c -> State.CHANGED.toString().equals(c.getOperation())));
    }

    @Test
    public void testOperationsOnComponents() {
        //given
        final String guid = "176eb6b8-c2a9-49f8-8c67-6c4883ca01bf";
        final CfServiceArtifact cfArtifactBefore = createCfServiceArtifact("yarn", Optional.empty(), Optional.of(date), Optional.of("description of service"));
        final CdhServiceArtifact cdhArtifactBefore = createCdhServiceArtifact(Optional.of("hue"), Optional.of("started"));
        final CfApplicationArtifact appArtifactBefore = createCfApplicationArtifact("service-catalog", Optional.of(guid), Optional.of(date));
        final PlatformSnapshot before = createPlatformSnapshot(1L, date, ImmutableList.of(cfArtifactBefore), ImmutableList.of(cdhArtifactBefore), ImmutableList.of(appArtifactBefore));

        final CfServiceArtifact cfArtifactAfter = createCfServiceArtifact("yarn", Optional.of(guid), Optional.empty(), Optional.empty());
        final CdhServiceArtifact cdhArtifactAfter = createCdhServiceArtifact(Optional.empty(), Optional.empty());
        final CfApplicationArtifact appArtifactAfter = createCfApplicationArtifact("service-catalog", Optional.of(guid), Optional.of(DateUtils.addHours(date, 5)));
        final PlatformSnapshot after = createPlatformSnapshot(2L, date, ImmutableList.of(cfArtifactAfter), ImmutableList.of(cdhArtifactAfter), ImmutableList.of(appArtifactAfter));

        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);

        //when
        final FlattenPlatformSnapshotDiff diff = (FlattenPlatformSnapshotDiff) service.diff(before.getId(), after.getId());

        //then
        assertTrue(diff.getComponents().stream().filter(c -> "yarn".equalsIgnoreCase(c.getArtifact())).allMatch(c -> State.ADDED.toString().equals(c.getOperation())));
        assertTrue(diff.getComponents().stream().filter(c -> "hue".equalsIgnoreCase(c.getArtifact())).allMatch(c -> State.REMOVED.toString().equals(c.getOperation())));
        assertTrue(diff.getComponents().stream().filter(c -> "service-catalog".equalsIgnoreCase(c.getArtifact())).allMatch(c -> State.CHANGED.toString().equals(c.getOperation())));
    }

    private void mockRepository(PlatformSnapshot after, PlatformSnapshot before) {
        when(repository.findOne(after.getId())).thenReturn(after);
        when(repository.findOne(before.getId())).thenReturn(before);
    }

    private CfServiceArtifact createCfServiceArtifact(String label, Optional<String> guid, Optional<Date> updatedAtDate,
                                                      Optional<String> description) {
        final CfServiceArtifact artifact = new CfServiceArtifact();
        artifact.setCreatedAt(date);
        artifact.setDescription(description.orElse(null));
        artifact.setGuid(guid.orElse(null));
        artifact.setId(1L);
        artifact.setLabel(label);
        artifact.setUpdatedAt(updatedAtDate.orElse(null));
        return artifact;
    }

    private CdhServiceArtifact createCdhServiceArtifact(Optional<String> name, Optional<String> state) {
        final CdhServiceArtifact artifact = new CdhServiceArtifact();
        artifact.setEntityStatus(state.orElse(null));
        artifact.setHealthSummary("good");
        artifact.setId(1L);
        artifact.setName(name.orElse(null));
        artifact.setServiceState("started");
        artifact.setType("hue");
        return artifact;
    }

    private CfApplicationArtifact createCfApplicationArtifact(String name, Optional<String> guid, Optional<Date> updatedAtDate) {
        final CfApplicationArtifact artifact = new CfApplicationArtifact();
        artifact.setBuildpack("java-buildpack");
        artifact.setCommand("command");
        artifact.setCreatedAt(date);
        artifact.setDetectedBuildpack("java-buildpack");
        artifact.setDetectedStartCommand("start-command");
        artifact.setDiskQuota(1024L);
        artifact.setGuid(guid.orElse(null));
        artifact.setHealthCheckTimeout(180L);
        artifact.setHealthCheckType("port");
        artifact.setId(1L);
        artifact.setInstances(1L);
        artifact.setMemory(512L);
        artifact.setName(name);
        artifact.setOrganization("test-org");
        artifact.setScope("core");
        artifact.setSpace("test-space");
        artifact.setState("started");
        artifact.setUpdatedAt(updatedAtDate.orElse(null));
        artifact.setVersion("0.2.3");
        return artifact;
    }

    private PlatformSnapshot createPlatformSnapshot(long id, Date date, Collection<CfServiceArtifact> cfArtifacts,
                                                    Collection<CdhServiceArtifact> cdhArtifacts, Collection<CfApplicationArtifact> cfApplicationArtifacts) {
        return PlatformSnapshot.builder().id(id)
                .platformVersion("0.8")
                .createdAt(date)
                .cfVersion("0.7.1")
                .cfServices(cfArtifacts)
                .cdhVersion("0.4.2")
                .cdhServices(cdhArtifacts)
                .applications(cfApplicationArtifacts).build();
    }

    private <T> T readObjectFromFile(ObjectMapper mapper, String resource, Class<T> clazz) throws IOException {
        return mapper.readValue(getClass().getClassLoader().getResource(resource), clazz);
    }

}