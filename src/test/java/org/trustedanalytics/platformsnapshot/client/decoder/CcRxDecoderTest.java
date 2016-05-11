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
package org.trustedanalytics.platformsnapshot.client.decoder;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.trustedanalytics.platformsnapshot.client.CfOperations;
import org.trustedanalytics.platformsnapshot.client.CfRxClient;
import org.trustedanalytics.platformsnapshot.client.LocalDateTimeDeserializer;
import org.trustedanalytics.platformsnapshot.client.entity.CfInfo;
import org.trustedanalytics.platformsnapshot.client.entity.CfService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.reflect.TypeToken;

import feign.Response;
import feign.Response.Body;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import rx.Observable;
import rx.observers.TestSubscriber;

public class CcRxDecoderTest {

    private static ObjectMapper MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(new LowerCaseWithUnderscoresStrategy())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(new SimpleModule()
            .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer()));

    @Test
    public void testDecodeNotPaginatedResource() throws IOException {
        // given
        final CfOperations client = mockCfOperations();
        final CcRxDecoder decoder = new CcRxDecoder(client, MAPPER);

        // when
        @SuppressWarnings("unchecked")
        Observable<CfInfo> cfInfo =
            (Observable<CfInfo>) decoder.decode(mockResponse(200, "v2_info.json"),
                (new TypeToken<Observable<CfInfo>>() {}).getType());

        // then
        final TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        cfInfo.map(CfInfo::getApiVersion).subscribe(testSubscriber);

        testSubscriber.assertValues("2.29.0");
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testDecodePaginatedResourceSinglePage() throws IOException {
        // given
        final CfOperations client = mockCfOperations();
        final CcRxDecoder decoder = new CcRxDecoder(client, MAPPER);

        // when
        @SuppressWarnings("unchecked")
        Observable<CfService> services =
            (Observable<CfService>) decoder.decode(mockResponse(200, "v2_services_not_paginated.json"),
                (new TypeToken<Observable<CfService>>() {}).getType());

        // then
        final TestSubscriber<UUID> testSubscriber = new TestSubscriber<>();
        services.map(service -> service.getMetadata().getGuid()).subscribe(testSubscriber);

        testSubscriber.assertValues(UUID.fromString("9b213bd9-a54f-4f41-abb4-dd12e7e50814"));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testDecodePaginatedResourceMultiplePages() throws IOException {
        // given
        final CfOperations client = mockCfOperations();
        final CcRxDecoder decoder = new CcRxDecoder(client, MAPPER);

        // when
        @SuppressWarnings("unchecked")
        Observable<CfService> services =
            (Observable<CfService>) decoder.decode(mockResponse(200, "v2_services_paginated.json"),
                (new TypeToken<Observable<CfService>>() {}).getType());

        // then
        final TestSubscriber<UUID> testSubscriber = new TestSubscriber<>();
        services.map(service -> service.getMetadata().getGuid()).subscribe(testSubscriber);

        testSubscriber.assertValues(UUID.fromString("9b213bd9-a54f-4f41-abb4-dd12e7e50814"));
        verify(client).getServices(any());
    }

    private Response mockResponse(int status, String resource) throws IOException {
        final Body body = mock(Body.class);
        final ClassPathResource res = new ClassPathResource(resource);

        when(body.asInputStream()).thenReturn(res.getInputStream());
        when(body.asReader()).thenReturn(new InputStreamReader(res.getInputStream()));

        return Response.create(status, "reason", new HashMap<>(), body);
    }

    private CfOperations mockCfOperations() {
        final CfRxClient client = mock(CfRxClient.class);
        when(client.getServices(any())).thenReturn(Observable.empty());
        return client;
    }

}
