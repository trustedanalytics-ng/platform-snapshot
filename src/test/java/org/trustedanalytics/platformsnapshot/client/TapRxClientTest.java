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

package org.trustedanalytics.platformsnapshot.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.CharStreams;
import feign.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.platformsnapshot.client.entity.TapApplication;
import org.trustedanalytics.platformsnapshot.client.entity.TapInfo;
import org.trustedanalytics.platformsnapshot.client.entity.TapService;
import rx.Observable;

import java.io.IOException;
import java.io.InputStreamReader;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class TapRxClientTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TapRxClientTest.class);

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule();

    TapOperations client;

    @Before
    public void before() {
         client = new TapRxClient(builder -> builder
                        .logLevel(Logger.Level.BASIC), "http://localhost:" + wireMockRule.port());
    }

    @Test
    public void testGetServicesCatalog() throws IOException {

        final String pageJson = loadJson("v3offerings.json");

        stubFor(get(urlPathEqualTo("/v3/offerings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson)));
        Observable<TapService>  services = client.getServices();
        TapService s = services.toBlocking().first();
        assertEquals(s.getName(), "gearpumpdashboard");
        LOGGER.info("Tap service {}", s);

    }

    @Test
    public void testGetInfo() throws IOException {

        final String pageJson = loadJson("v3info.json");

        stubFor(get(urlPathEqualTo("/v3/platform_info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson)));
        Observable<TapInfo>  info = client.getTapInfo();
        TapInfo my_info = info.toBlocking().first();
        assertEquals(my_info.getPlatformVersion(), "0.8.0.2417");
        LOGGER.info("Tap service {}", my_info);

    }

    @Test
    public void testGetApplications() throws IOException {

        final String pageJson = loadJson("application.json");

        stubFor(get(urlPathEqualTo("/v3/applications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson)));
        Observable<TapApplication>  apps = client.getApplications();
        TapApplication app = apps.toBlocking().first();
        assertEquals("my-python-app",app.getName());
        LOGGER.info("Tap app {}", app);

    }

    private String loadJson(String name) {
        try {
            return CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(name)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file name: " + name);
        }
    }
}
