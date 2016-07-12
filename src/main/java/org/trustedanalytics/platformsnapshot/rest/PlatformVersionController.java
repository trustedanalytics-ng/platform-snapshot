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
package org.trustedanalytics.platformsnapshot.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import org.trustedanalytics.platformsnapshot.model.PlatformVersion;
import org.trustedanalytics.platformsnapshot.service.PlatformVersionSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import io.swagger.annotations.ApiOperation;

@RestController
public class PlatformVersionController {
    private final PlatformVersionSupplier supplier;

    @Autowired
    public PlatformVersionController(PlatformVersionSupplier supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    @ApiOperation(
        value = "Get platform components versions",
        notes = "Privilege level: Consumer of this endpoint must authenticated."
    )
    @RequestMapping(value = "/rest/v1/versions", method = GET, produces = APPLICATION_JSON_VALUE)
    public PlatformVersion getPlatformVersion() {
        return supplier.get().toBlocking().single();
    }
}
