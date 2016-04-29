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
package org.trustedanalytics.platformsnapshot.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import org.trustedanalytics.platformsnapshot.client.entity.CfApplication;
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.client.entity.CfOrganization;
import org.trustedanalytics.platformsnapshot.client.entity.CfService;
import org.trustedanalytics.platformsnapshot.client.entity.CfSpace;
import rx.Observable;

import java.net.URI;
import java.util.UUID;

@Headers("Content-Type: application/json")
public interface CfOperations {

    @RequestLine("GET /v2/spaces/{space}/apps")
    Observable<CfApplication> getApplications(@Param("space") UUID space);

    @RequestLine("GET")
    Observable<CfApplication> getApplications(URI uri);

    @RequestLine("GET /v2/spaces")
    Observable<CfSpace> getSpaces();

    @RequestLine("GET")
    Observable<CfSpace> getSpaces(URI uri);

    @RequestLine("GET /v2/organizations?q={orgName}")
    Observable<CfOrganization> getOrganization(@Param("orgName") String orgName);

    @RequestLine("GET /v2/info")
    Observable<CfInfo> getCfInfo();

    @RequestLine("GET /v2/services")
    Observable<CfService> getServices();

    @RequestLine("GET")
    Observable<CfService> getServices(URI uri);
}
