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
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.apache.http.conn.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.trustedanalytics.platformsnapshot.client.cdh.CdhOperations;
import org.trustedanalytics.platformsnapshot.service.ClouderaConfiguration;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@Configuration
@Profile("cloud")
public class CdhConfiguration {

    @Bean
    public CdhOperations cdhOperations(SSLContext sslContext, ClouderaConfiguration clouderaConfiguration) {

        ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Client sslClient = new Client.Default((SSLSocketFactory)SSLSocketFactory.getDefault(),null);

        return Feign.builder()
            .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .logger(new Slf4jLogger(CdhOperations.class))
                .requestInterceptor(new BasicAuthRequestInterceptor(clouderaConfiguration.getUser(), clouderaConfiguration.getPassword()))
                .client(sslClient)
                .logLevel(Logger.Level.FULL)
                .target(CdhOperations.class, String.format("https://%s:%s", clouderaConfiguration.getHost(), clouderaConfiguration.getPort()));
    }

    @Bean
    public SSLContext getSSLContext(ClouderaConfiguration clouderaConfiguration) throws IOException, GeneralSecurityException {
        if (clouderaConfiguration == null) {
            throw new IllegalStateException("Empty cloudera configuration");
        }
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(clouderaConfiguration.getStore())) {
            ks.load(fis, clouderaConfiguration.getStorePassword().toCharArray());
        }
        SSLContext sslContext =  SSLContexts.custom().loadTrustMaterial(ks).build();
        SSLContext.setDefault(sslContext);
        return sslContext;
    }
}