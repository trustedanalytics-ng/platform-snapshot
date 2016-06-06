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

import org.trustedanalytics.platformsnapshot.client.CfOperations;
import org.trustedanalytics.platformsnapshot.client.CfRxClient;
import org.trustedanalytics.platformsnapshot.client.LocalDateTimeDeserializer;
import org.trustedanalytics.platformsnapshot.client.PlatformContextOperations;
import org.trustedanalytics.platformsnapshot.security.OAuth2TokenSupplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

@Configuration
public class ApplicationConfiguration {

    @Value("${services.platform-context}")
    private String platformContextUrl;

    @Value("${spring.oauth2.resource.api}")
    private String cfApiUrl;

    @Bean
    public OAuth2TokenSupplier oAuth2TokenSupplier() {
        return new OAuth2TokenSupplier();
    }

    @Bean
    public OAuth2ClientContext oauth2ClientContext() {
        return new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest());
    }

    @Bean
    @ConfigurationProperties("spring.oauth2.client")
    public OAuth2ProtectedResourceDetails clientCredentials() {
        return new ClientCredentialsResourceDetails();
    }

    @Bean
    public OAuth2RestTemplate clientRestTemplate() {
        OAuth2RestTemplate template = new OAuth2RestTemplate(clientCredentials());
        ClientCredentialsAccessTokenProvider provider = new ClientCredentialsAccessTokenProvider();
        template.setAccessTokenProvider(provider);
        return template;
    }

    @Bean
    public LocalDateTimeDeserializer localDateTimeDeserializer() {
        return new LocalDateTimeDeserializer();
    }

    @Bean
    public PlatformContextOperations platformContextOperations() {
        final String token = clientRestTemplate().getAccessToken().toString();
        return Feign.builder()
                    .encoder(new JacksonEncoder(objectMapper()))
                    .decoder(new JacksonDecoder(objectMapper()))
                    .logger(new Slf4jLogger(PlatformContextOperations.class))
                    .options(new Request.Options(30_1000, 10_1000))
                    .requestInterceptor(template -> template.header("Authorization", "bearer " + token))
                    .logLevel(Logger.Level.BASIC)
                    .target(PlatformContextOperations.class, platformContextUrl);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public CfOperations cfRxClient() {
        final String token = clientRestTemplate().getAccessToken().toString();
        return new CfRxClient(builder -> builder
            .requestInterceptor(template -> template.header("Authorization", "bearer " + token))
            .logLevel(Logger.Level.BASIC), cfApiUrl);
    }


}
