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
package org.trustedanalytics.platformsnapshot.scheduler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.CharStreams;
import com.opentable.db.postgres.embedded.EmbeddedPostgreSQLRule;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.trustedanalytics.platformsnapshot.Application;
import org.trustedanalytics.platformsnapshot.client.TapOperations;
import org.trustedanalytics.platformsnapshot.client.TapRxClient;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.model.PlatformSnapshot;
import org.trustedanalytics.platformsnapshot.persistence.PlatformSnapshotRepository;
import org.trustedanalytics.platformsnapshot.service.PlatformSnapshotScheduler;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*

integration test, not launched by default mvn profile

can be run by:
mvn verify -P integration-test


 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PlatformSnapshotSchedulerIntegrationTest.PostgresTestConfiguration.class, Application.class})
public class PlatformSnapshotSchedulerIntegrationTest {

    @Autowired
    PlatformSnapshotRepository platformSnapshotRepository;

    @ClassRule
    public static EmbeddedPostgreSQLRule pg = new EmbeddedPostgreSQLRule();

    private PlatformSnapshotScheduler platformSnapshotScheduler;

    TapOperations tapOperations;
    CdhOperations cdhOperations;

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule();


    @Before
    public void before() {
        tapOperations = new TapRxClient(builder -> builder
                .logLevel(Logger.Level.BASIC), "http://localhost:" + wireMockRule.port());

        ObjectMapper objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        cdhOperations =  Feign.builder()
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .logger(new Slf4jLogger(CdhOperations.class))
                .options(new Request.Options(30_1000, 10_1000))
                .logLevel(Logger.Level.BASIC)
                .target(CdhOperations.class, String.format("http://%s:%s", "localhost", wireMockRule.port()));

        platformSnapshotScheduler = new PlatformSnapshotScheduler(tapOperations, platformSnapshotRepository, cdhOperations);

        final String clusters= loadJson("cdhcluster.json");


        stubFor(get(urlPathEqualTo("/api/v11/clusters"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(clusters)));

        final String cdhservices= loadJson("cdhservices.json");
        stubFor(get(urlPathEqualTo("/api/v11/clusters/CDH-cluster/services"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(cdhservices)));

        final String pageJson = loadJson("catalog.json");

        stubFor(get(urlPathEqualTo("/v1/catalog"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(pageJson)));

        final String infoJson = loadJson("v1_info.json");

        stubFor(get(urlPathEqualTo("/v1/platform_info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(infoJson)));

        final String appsJson = loadJson("application.json");

        stubFor(get(urlPathEqualTo("/v1/applications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(appsJson)));

    }

    @Test
    @Transactional
    public void testTriggerOneSnapshotAndReadPlatformVersion() throws InterruptedException, SQLException {
        platformSnapshotScheduler.trigger();
        Thread.sleep(5000);
        PlatformSnapshot snapshot = platformSnapshotRepository.findTopByOrderByCreatedAtDesc();
        assertTrue("0.8.1388".equals(snapshot.getPlatformVersion()));
    }

    @Test
    @Transactional
    public void testTriggerOneSnapshotAndReadApplicationsAndServices() throws InterruptedException, SQLException {
        platformSnapshotScheduler.trigger();
        Thread.sleep(5000);
        PlatformSnapshot snapshot = platformSnapshotRepository.findTopByOrderByCreatedAtDesc();
        assertEquals(1, snapshot.getApplications().size());
        assertEquals(2, snapshot.getTapServices().size());
        assertEquals(14, snapshot.getCdhServices().size());
    }

    public static class PostgresTestConfiguration {
        @Bean
        public DataSource getDataSource() {
            DataSource data =  pg.getEmbeddedPostgreSQL().getPostgresDatabase();
            return data;
        }
    }

    private String loadJson(String name) {
        try {
            return CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(name)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file name: " + name);
        }
    }
}
