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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotConfiguration;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshotDiff;
import org.trustedanalytics.platformsnapshot.model.Scope;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotTransactions;
import org.trustedanalytics.platformsnapshot.service.PlatformSnapshotDiffService;
import org.trustedanalytics.platformsnapshot.service.PlatformSnapshotScheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.annotations.ApiOperation;

@RestController
public class PlatformSnapshotController {
    private final PlatformSnapshotRepository platformSnapshotRepository;
    private final PlatformSnapshotScheduler platformSnapshotScheduler;
    private final PlatformSnapshotTransactions platformSnapshotTransactions;
    private final PlatformSnapshotDiffService platformSnapshotDiffService;

    @Autowired
    public PlatformSnapshotController(PlatformSnapshotRepository platformSnapshotRepository,
                                      PlatformSnapshotScheduler platformSnapshotScheduler,
                                      PlatformSnapshotTransactions platformSnapshotTransactions,
                                      PlatformSnapshotDiffService platformSnapshotDiffService) {
        this.platformSnapshotRepository = platformSnapshotRepository;
        this.platformSnapshotScheduler = platformSnapshotScheduler;
        this.platformSnapshotTransactions = platformSnapshotTransactions;
        this.platformSnapshotDiffService = platformSnapshotDiffService;
    }

    @ApiOperation(
        value = "Get platform snapshots",
        notes = "Privilege level: Consumer of this endpoint must be an admin."
    )
    @RequestMapping(value = "/rest/v1/snapshots", method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<PlatformSnapshot> getPlatformSnapshot(
        @RequestParam("from") Optional<LocalDateTime> fromDate,
        @RequestParam("to") Optional<LocalDateTime> toDate,
        @RequestParam("scope") Optional<Scope> scope) {

        final LocalDateTime from = fromDate.orElse(LocalDateTime.now().minusDays(7));
        final LocalDateTime to = toDate.orElse(LocalDateTime.now());

        return platformSnapshotRepository.findByDates(Date.from(from.toInstant(ZoneOffset.UTC)), Date
            .from(to.toInstant(ZoneOffset.UTC)))
            .stream()
            .map(snapshot -> snapshot.filter(scope.orElse(Scope.ALL)))
            .collect(Collectors.toList());
    }

    @ApiOperation(
        value = "Get platform snapshot by id",
        notes = "Privilege level: Consumer of this endpoint must be an admin."
    )
    @RequestMapping(value = "/rest/v1/snapshots/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    public PlatformSnapshot getPlatformSnapshot(@PathVariable("id") Long id) {
        return platformSnapshotRepository.findOne(id);
    }

    @ApiOperation(
        value = "Trigger platform snapshot generation",
        notes = "Privilege level: Consumer of this endpoint must be an admin."
    )
    @RequestMapping(value = "/rest/v1/snapshots/trigger", method = GET, produces = APPLICATION_JSON_VALUE)
    public void triggerPlatformSnapshot() {
        platformSnapshotScheduler.trigger();
    }

    @ApiOperation(
        value = "Get platform snapshot configuration",
        notes = "Privilege level: Consumer of this endpoint must be an user."
    )
    @RequestMapping(value = "/rest/v1/configuration", method = GET, produces = APPLICATION_JSON_VALUE)
    public PlatformSnapshotConfiguration getPlatformConfiguration() {
        return new PlatformSnapshotConfiguration(Arrays.asList(Scope.values())
            .stream()
            .map(Enum::name)
            .collect(Collectors.toList()));
    }

    @ApiOperation(
        value = "Delete platform snapshots older then",
        notes = "Privilege level: Consumer of this endpoint must be an admin."
    )
    @RequestMapping(value = "/rest/v1/snapshots/delete", method = GET, produces = APPLICATION_JSON_VALUE)
    public void deletePlatformSnapshot(@RequestParam("date") LocalDateTime date) {
        platformSnapshotTransactions.deleteOlderThen(Date.from(date.toInstant(ZoneOffset.UTC)));
    }

    @ApiOperation(
            value = "Perform difference between snapshots to identify what has changed.",
            notes = "Privilege level: Consumer of this endpoint must be an admin."
    )
    @RequestMapping(value = "/rest/v1/snapshots/{idBefore}/diff/{idAfter}", method = GET, produces = APPLICATION_JSON_VALUE)
    public PlatformSnapshotDiff compareSnapshots(
            @PathVariable("idBefore") long idBefore,
            @PathVariable("idAfter") long idAfter,
            @RequestParam(value = "aggregateBy") Optional<String> aggregateBy) {

        return aggregateBy.filter("type"::equalsIgnoreCase).map(type ->
                platformSnapshotDiffService.diffByType(idBefore, idAfter))
                .orElse(platformSnapshotDiffService.diff(idBefore, idAfter));
    }

}
