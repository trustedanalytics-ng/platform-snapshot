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

import org.trustedanalytics.platformsnapshot.client.entity.CfApplication;
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.client.entity.CfOrganization;
import org.trustedanalytics.platformsnapshot.client.entity.CfService;
import org.trustedanalytics.platformsnapshot.client.entity.CfSpace;
import org.trustedanalytics.platformsnapshot.client.decoder.CcRxDecoder;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import feign.Feign;
import feign.Feign.Builder;
import feign.Request;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import rx.Observable;

public class CfRxClient implements CfOperations {

    private final CfOperations cfOperations;

    public CfRxClient(Function<Builder, Builder> customizations, String apiBaseUrl) {

        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

        final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(simpleModule);

        cfOperations = customizations.apply(Feign.builder()
            .encoder(new JacksonEncoder(mapper))
            .decoder(new CcRxDecoder(this, mapper))
            .logger(new Slf4jLogger(CfRxClient.class))
            .options(new Request.Options(30_1000, 10_1000)))
            .target(CfOperations.class, apiBaseUrl);
    }

    @Override
    public Observable<CfApplication> getApplications(UUID space) {
        return cfOperations.getApplications(space);
    }

    @Override
    public Observable<CfApplication> getApplications(URI uri) {
        return cfOperations.getApplications(uri);
    }

    @Override
    public Observable<CfSpace> getSpaces() {
        return cfOperations.getSpaces();
    }

    @Override
    public Observable<CfSpace> getSpaces(URI uri) {
        return cfOperations.getSpaces(uri);
    }

    @Override
    public Observable<CfOrganization> getOrganization(String orgName) {
        return cfOperations.getOrganization(orgName);
    }

    @Override
    public Observable<CfInfo> getCfInfo() {
        return cfOperations.getCfInfo();
    }

    @Override
    public Observable<CfService> getServices() {
        return cfOperations.getServices();
    }

    @Override
    public Observable<CfService> getServices(URI uri) {
        return cfOperations.getServices(uri);
    }
}
