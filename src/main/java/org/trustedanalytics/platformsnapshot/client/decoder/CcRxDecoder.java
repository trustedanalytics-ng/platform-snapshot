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
package org.trustedanalytics.platformsnapshot.client.decoder;

import org.trustedanalytics.platformsnapshot.client.CfOperations;
import org.trustedanalytics.platformsnapshot.client.entity.CfPage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;

import feign.Response;
import feign.codec.Decoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

import rx.Observable;

public class CcRxDecoder implements Decoder {
    private static final int BUFFER_SIZE = 32;
    private final CfOperations client;
    private final CcRxPageResolver resolver;
    private final ObjectMapper mapper;

    public CcRxDecoder(CfOperations client, ObjectMapper mapper) {
        this.client = Objects.requireNonNull(client, "client");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.resolver = new CcRxPageResolver();
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.body() == null) {
            return null;
        }
        Reader reader = response.body().asReader();
        if (!reader.markSupported()) {
            reader = new BufferedReader(reader, BUFFER_SIZE);
        }
        try {
            final char[] buffer = new char[BUFFER_SIZE];

            reader.mark(BUFFER_SIZE);
            if (reader.read(buffer) == -1) {
                return Observable.just(response);
            }

            reader.reset();
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            if (new String(buffer).contains("total_results")) {
                return concatPages(mapper.readValue(reader, mapper.constructType(toPageType(parameterizedType))));
            } else {
                return Observable.just(mapper.readValue(reader, mapper.constructType(toScalarType(parameterizedType))));
            }
        } catch (RuntimeJsonMappingException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
                throw IOException.class.cast(e.getCause());
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> concatPages(CfPage<T> cfPage) {
        final Observable<T> resources = Observable.from(cfPage.getResources());

        if (cfPage.getNextUrl() != null) {
            return resources.concatWith(Observable.defer(() -> (Observable<T>) resolver.apply(cfPage.getNextUrl(), client)));
        } else {
            return resources;
        }
    }

    private Type toPageType(ParameterizedType type) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return type.getActualTypeArguments();
            }

            @Override
            public Type getRawType() {
                return CfPage.class;
            }

            @Override
            public Type getOwnerType() {
                return type.getOwnerType();
            }
        };
    }

    private Type toScalarType(ParameterizedType type) {
        return type.getActualTypeArguments()[0];
    }
}
