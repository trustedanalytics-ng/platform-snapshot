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

import com.google.common.base.MoreObjects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    @Column(name="K8S_VERSION")
    private String k8sVersion;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private Collection<TapApplicationArtifact> applications;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private Collection<CdhServiceArtifact> cdhServices;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private Collection<TapServiceArtifact> tapServices;

    public PlatformSnapshot(Date createdAt,
                            String platformVersion,
                            Collection<TapApplicationArtifact> applications,
                            String cdhVersion,
                            String k8sVersion,
                            Collection<CdhServiceArtifact> cdhServices,
                            Collection<TapServiceArtifact> tapServices) {

        this.createdAt = createdAt;
        this.platformVersion = platformVersion;
        this.applications = applications;
        this.cdhServices = cdhServices;
        this.cdhVersion = cdhVersion;
        this.k8sVersion = k8sVersion;
        this.tapServices = tapServices;

        cdhServices.forEach(cdhService -> cdhService.setSnapshot(this));
        applications.forEach(application -> application.setSnapshot(this));
        tapServices.forEach(tapService -> tapService.setSnapshot(this));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("platformVersion", platformVersion)
                .add("createdAt", createdAt)
                .add("cdhVersion", cdhVersion)
                .add("k8sVersion", k8sVersion)
                .toString();
    }
}
