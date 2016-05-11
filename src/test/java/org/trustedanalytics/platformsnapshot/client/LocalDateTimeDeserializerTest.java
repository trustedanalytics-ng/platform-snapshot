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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class LocalDateTimeDeserializerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDateTimeDeserializerTest.class);

    @Mock
    DeserializationContext deserializationContext;

    LocalDateTimeDeserializer localDateTimeDeserializer;
    String correctDate;
    String incorrectDate;

    @Before
    public void setUp() {
        localDateTimeDeserializer = new LocalDateTimeDeserializer();
        correctDate = "2016-04-10T06:45:00Z";
        incorrectDate = "2016/04/10T06:45:00Z";
    }

    @Test
    public void deserialize_correctInput_returnDeserializedDate() {
        LocalDateTime actualDate = localDateTimeDeserializer._deserialize(correctDate, deserializationContext);
        assertEquals(actualDate, doConvert(correctDate));
    }

    @Test
    public void convert_correctInput_returnDeserializedDate() {
        LocalDateTime actualDate = localDateTimeDeserializer.convert(correctDate);
        assertEquals(actualDate, doConvert(correctDate));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserialize_incorrectInput_throwIllegalArgumentException() {
        localDateTimeDeserializer._deserialize(incorrectDate, deserializationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convert_incorrectInput_throwIllegalArgumentException() {
        localDateTimeDeserializer.convert(incorrectDate);
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
