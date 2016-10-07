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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import feign.Response;
import feign.codec.Decoder;
import rx.Observable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TapRxDecoder implements Decoder {
    private static final int BUFFER_SIZE = 32;
    private final ObjectMapper mapper;

    public TapRxDecoder(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
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
            if (new String(buffer).startsWith("[")) {
                return listToObservable(mapper.readValue(reader, mapper.constructType(toListType(parameterizedType))));
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
    private <T> Observable<T> listToObservable(List<T> list) {
        final Observable<T> resources = Observable.from(list);
        return resources;
    }

    private Type toListType(ParameterizedType type) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return type.getActualTypeArguments();
            }

            @Override
            public Type getRawType() {
                return ArrayList.class;
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