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

import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String platformVersion;

    private Date createdAt;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Collection<CfApplicationArtifact> applications;

    public PlatformSnapshot(Date createdAt, String platformVersion,
        Collection<CfApplicationArtifact> applications) {
        this.createdAt = createdAt;
        this.platformVersion = platformVersion;
        this.applications = applications;

        applications.forEach(application -> application.setSnapshot(this));
    }

    public PlatformSnapshot filter(Scope scope) {
        final Predicate<CfApplicationArtifact> predicate =
            app -> Scope.ALL.equals(scope) || scope.equals(app.getScope());

        return PlatformSnapshot.builder()
            .id(id)
            .createdAt(createdAt)
            .platformVersion(platformVersion)
            .applications(applications.stream().filter(predicate).collect(Collectors.toList()))
            .build();
    }
}
