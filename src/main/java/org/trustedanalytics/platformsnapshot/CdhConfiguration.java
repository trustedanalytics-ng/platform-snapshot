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
package org.trustedanalytics.platformsnapshot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;

import javax.validation.constraints.NotNull;

@Configuration
public class CdhConfiguration {

    @Value("${cloudera.user}")
    @NotNull
    @Getter
    private String user;

    @Value("${cloudera.password}")
    @NotNull
    @Getter
    private String password;

    @Value("${cloudera.address}")
    @NotNull
    @Getter
    private String host;

    @Value("${cloudera.port}")
    @NotNull
    @Getter
    private Integer port;

    @Bean
    public CdhOperations cdhOperations() {

        ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return Feign.builder()
            .encoder(new JacksonEncoder(objectMapper))
            .decoder(new JacksonDecoder(objectMapper))
            .logger(new Slf4jLogger(CdhOperations.class))
            .options(new Request.Options(30_1000, 10_1000))
            .requestInterceptor(new BasicAuthRequestInterceptor(user, password))
            .logLevel(Logger.Level.BASIC)
            .target(CdhOperations.class, String.format("http://%s:%s", host, port));
    }
}