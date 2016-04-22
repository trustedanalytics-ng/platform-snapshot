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
package org.trustedanalytics.platformsnapshot.client;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class LocalDateTimeDeserializer extends FromStringDeserializer<LocalDateTime> implements Converter<String, LocalDateTime> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDateTimeDeserializer.class);

    public LocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    protected LocalDateTime _deserialize(String value, DeserializationContext ctxt) throws IOException {
        return doConvert(value);
    }

    @Override
    public LocalDateTime convert(String value) {
        return doConvert(value);
    }

    private LocalDateTime doConvert(String value) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
        try {
            Date date = formatter.parse(value);
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
        } catch (ParseException e) {
            LOGGER.error("Could not parse date. ", e);
            throw new IllegalArgumentException("Could not parse date: " + value);
        }
    }
}
