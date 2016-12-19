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
import org.trustedanalytics.platformsnapshot.client.entity.TapService;
import org.trustedanalytics.platformsnapshot.model.TapApplicationArtifact;
import org.trustedanalytics.platformsnapshot.model.TapServiceArtifact;
import rx.Observable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class CreateArtifactsTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CreateArtifactsTest.class);

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule();

    TapOperations client;

    @Before
    public void before() {
        client = new TapRxClient(builder -> builder
                .logLevel(Logger.Level.BASIC), "http://localhost:" + wireMockRule.port());
    }

    @Test
    public void testCreateServiceArtifact() throws IOException {

        final String pageJson = loadJson("v3offerings.json");

        stubFor(get(urlPathEqualTo("/v3/offerings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson)));
        Observable<TapService>  services = client.getServices();
        TapService s = services.toBlocking().first();
        TapServiceArtifact serviceArtifact = new TapServiceArtifact(s);

        assertEquals("gearpumpdashboard",serviceArtifact.getLabel());

        LOGGER.info("Service created at: {}", serviceArtifact.getCreatedAt());
        LOGGER.info("Tap service artifact{}", serviceArtifact);
    }

    @Test
    public void testCreateApplicationArtifactWithCreationDate() throws IOException, ParseException {

        final String pageJson = loadJson("application.json");

        stubFor(get(urlPathEqualTo("/v3/applications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson)));
        Observable<TapApplication>  apps = client.getApplications();
        TapApplication app = apps.toBlocking().first();
        TapApplicationArtifact applicationArtifact = new TapApplicationArtifact(app);

        assertEquals(dateFromString("Tuesday, Oct 11, 2016 13:01:47 PM"),applicationArtifact.getCreatedAt());

        LOGGER.info("Tap application artifact {}", applicationArtifact);

    }

    private String loadJson(String name) {
        try {
            return CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(name)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file name: " + name);
        }
    }

    private Date dateFromString(String dateInString) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm:ss a");;

        Date date = formatter.parse(dateInString);
        return date;

    }
}
