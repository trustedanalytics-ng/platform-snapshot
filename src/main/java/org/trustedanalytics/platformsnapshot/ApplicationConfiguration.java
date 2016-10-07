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
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import feign.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.trustedanalytics.platformsnapshot.client.TapOperations;
import org.trustedanalytics.platformsnapshot.client.TapRxClient;
import org.trustedanalytics.platformsnapshot.client.LocalDateTimeDeserializer;
import org.trustedanalytics.platformsnapshot.client.uaa.CachedUaaOperations;
import org.trustedanalytics.platformsnapshot.client.uaa.OAuth2PrivilegedInterceptor;
import org.trustedanalytics.platformsnapshot.client.uaa.UaaOperations;
import org.trustedanalytics.platformsnapshot.security.OAuth2TokenSupplier;

@Configuration
@Profile("cloud")
public class ApplicationConfiguration {

    @Value("${api.service}")
    private String tapApiUrl;

    @Bean
    public OAuth2TokenSupplier oAuth2TokenSupplier() {
        return new OAuth2TokenSupplier();
    }

    @Bean
    @ConfigurationProperties("spring.oauth2.client")
    public OAuth2ProtectedResourceDetails clientCredentials() {

        return new ClientCredentialsResourceDetails();
    }

    @Bean
    public LocalDateTimeDeserializer localDateTimeDeserializer() {
        return new LocalDateTimeDeserializer();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public TapOperations tapRxClient(OAuth2PrivilegedInterceptor oauth2PrivilegedInterceptor) {
        return new TapRxClient(builder -> builder
            .requestInterceptor(oauth2PrivilegedInterceptor)
            .logLevel(Logger.Level.BASIC), tapApiUrl);
    }

    @Bean
    protected OAuth2PrivilegedInterceptor oauth2PrivilegedInterceptor(UaaOperations uaaOperations) {
        return new OAuth2PrivilegedInterceptor(uaaOperations);
    }

    @Bean
    public UaaOperations uaaOperations(@Value("${uaaUri}") String uaaUri, OAuth2ProtectedResourceDetails clientCredentials) {
        return new CachedUaaOperations( uaaUri, clientCredentials.getClientId(), clientCredentials.getClientSecret());
    }

}
