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
package org.trustedanalytics.platformsnapshot.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "PLATFORM_SNAPSHOT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSnapshot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="PLATFORM_VERSION")
    private String platformVersion;

    @Column(name="CREATED_At")
    private Date createdAt;

    @Column(name="CDH_VERSION")
    private String cdhVersion;

    @Column(name="CF_VERSION")
    private String cfVersion;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private Collection<CfApplicationArtifact> applications;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private Collection<CdhServiceArtifact> cdhServices;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private Collection<CfServiceArtifact> cfServices;

    public PlatformSnapshot(Date createdAt,
                            String platformVersion,
                            Collection<CfApplicationArtifact> applications,
                            String cdhVersion,
                            String cfVersion,
                            Collection<CdhServiceArtifact> cdhServices,
                            Collection<CfServiceArtifact> cfServices) {

        this.createdAt = createdAt;
        this.platformVersion = platformVersion;
        this.applications = applications;
        this.cdhServices = cdhServices;
        this.cdhVersion = cdhVersion;
        this.cfVersion = cfVersion;
        this.cfServices = cfServices;

        cdhServices.forEach(cdhService -> cdhService.setSnapshot(this));
        applications.forEach(application -> application.setSnapshot(this));
        cfServices.forEach(cfService -> cfService.setSnapshot(this));
    }

    public PlatformSnapshot filter(Scope scope) {
        final Predicate<CfApplicationArtifact> predicate =
            app -> Scope.ALL.equals(scope) || scope.equals(app.getScope());

        return PlatformSnapshot.builder()
            .id(id)
            .createdAt(createdAt)
            .platformVersion(platformVersion)
            .cdhVersion(cdhVersion)
            .cfVersion(cfVersion)
            .applications(applications.stream().filter(predicate).collect(Collectors.toList()))
            .cdhServices(cdhServices)
            .cfServices(cfServices)
            .build();
    }
}
