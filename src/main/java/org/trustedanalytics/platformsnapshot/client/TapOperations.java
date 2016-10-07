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
import org.trustedanalytics.platformsnapshot.client.entity.*;
import rx.Observable;

import java.net.URI;

@Headers("Content-Type: application/json")
public interface TapOperations {

    @RequestLine("GET /v1/applications")
    Observable<TapApplication> getApplications();

    @RequestLine("GET")
    Observable<TapApplication> getApplications(URI uri);

    @RequestLine("GET /v2/organizations?q={orgName}")
    Observable<TapOrganization> getOrganization(@Param("orgName") String orgName);

    @RequestLine("GET /v1/catalog")
    Observable<TapService> getServices();

    @RequestLine("GET")
    Observable<TapService> getServices(URI uri);

    @RequestLine("GET /v1/platform_info")
    Observable<TapInfo> getTapInfo();
}
