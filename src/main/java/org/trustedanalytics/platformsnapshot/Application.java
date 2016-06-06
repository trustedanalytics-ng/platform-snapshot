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


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.cloud.security.oauth2.resource.EnableOAuth2Resource;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter;

import java.util.HashMap;
import java.util.Map;

@EnableOAuth2Resource
@SpringBootApplication
public class Application extends JpaBaseConfiguration {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new OpenJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        Map<String,Object> properties = new HashMap<>();
        properties.put("openjpa.DynamicEnhancementAgent","false");
        properties.put("openjpa.RuntimeUnenhancedClasses","supported");
        return new HashMap<>();
    }
}
