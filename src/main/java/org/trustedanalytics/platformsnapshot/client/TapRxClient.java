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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import feign.Feign;
import feign.Feign.Builder;
import feign.Request;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.platformsnapshot.client.decoder.TapRxDecoder;
import org.trustedanalytics.platformsnapshot.client.entity.*;
import rx.Observable;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.function.Function;

public class TapRxClient implements TapOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapRxClient.class);

    private final TapOperations tapOperations;

    public TapRxClient(Function<Builder, Builder> customizations, String apiBaseUrl) {

        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

        final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(simpleModule);

        tapOperations = customizations.apply(Feign.builder()
            .encoder(new JacksonEncoder(mapper))
            .decoder(new TapRxDecoder(mapper))
            .logger(new Slf4jLogger(TapRxClient.class))
            .options(new Request.Options(30_1000, 10_1000)))
            .target(TapOperations.class, apiBaseUrl);
    }

    @Override
    public Observable<TapApplication> getApplications() {
        return tapOperations.getApplications();
    }

    @Override
    public Observable<TapApplication> getApplications(URI uri) {
        return tapOperations.getApplications(uri);
    }

    @Override
    public Observable<TapOrganization> getOrganization(String orgName) {
        return tapOperations.getOrganization(orgName);
    }

    @Override
    public Observable<TapInfo> getTapInfo() {
        return tapOperations.getTapInfo();
    }

    @Override
    public Observable<TapService> getServices() {
         return tapOperations.getServices().doOnNext(tapService -> LOGGER.info("service: {}", tapService));
    }

    @Override
    public Observable<TapService> getServices(URI uri) {
        return tapOperations.getServices(uri);
    }
}
