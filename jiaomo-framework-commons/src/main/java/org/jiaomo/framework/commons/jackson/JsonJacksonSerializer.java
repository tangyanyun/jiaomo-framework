/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jiaomo.framework.commons.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.jiaomo.framework.commons.function.ThrowingSupplier;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;


public class JsonJacksonSerializer {
    public static final JsonJacksonSerializer INSTANCE = new JsonJacksonSerializer();

    private final ObjectMapper objectMapper;

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public byte[] serialize(Object obj) {
        return ThrowingSupplier.get(() -> objectMapper.writeValueAsBytes(obj));
    }
    public String serializeAsString(Object obj) {
        return ThrowingSupplier.get(() -> objectMapper.writeValueAsString(obj));
    }

    public <T> T deserialize(byte[] bytes,Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return ThrowingSupplier.get(() -> objectMapper.readValue(bytes,clazz));
    }
    public <T> T deserialize(String str,Class<T> clazz) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        return ThrowingSupplier.get(() -> objectMapper.readValue(str,clazz));
    }
    public <T> T deserialize(String str, TypeReference<T> valueTypeRef) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        return ThrowingSupplier.get(() -> objectMapper.readValue(str,valueTypeRef));
    }

    private JsonJacksonSerializer() {
//      this.objectMapper = new ObjectMapper();
        this.objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
        init(this.objectMapper);
        initTypeInclusion(this.objectMapper);

        serializeAsString(BigDecimal.TEN);
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NON_PRIVATE,
            getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            setterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class ThrowableMixIn {
    }

    protected void init(ObjectMapper objectMapper) {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(objectMapper.getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
//      objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        objectMapper.addMixIn(Throwable.class, ThrowableMixIn.class);
    }

    protected void initTypeInclusion(ObjectMapper objectMapper) {
        TypeResolverBuilder<?> mapTyper = new ObjectMapper.DefaultTypeResolverBuilder(
                ObjectMapper.DefaultTyping.NON_FINAL, LaissezFaireSubTypeValidator.instance) {
            public boolean useForType(JavaType javaType) {
                switch (_appliesFor) {
                    case NON_CONCRETE_AND_ARRAYS:
                        while (javaType.isArrayType()) {
                            javaType = javaType.getContentType();
                        }
                    case OBJECT_AND_NON_CONCRETE:
                        return (javaType.getRawClass() == Object.class) || !javaType.isConcrete();
                    case NON_FINAL:
                        while (javaType.isArrayType()) {
                            javaType = javaType.getContentType();
                        }
                        if (javaType.getRawClass() == Long.class)
                            return true;
                        if (javaType.getRawClass() == XMLGregorianCalendar.class)
                            return false;
                        return !javaType.isFinal();
                    default:
                        return javaType.getRawClass() == Object.class;
                }
            }
        };
        mapTyper.init(JsonTypeInfo.Id.CLASS, null);
        mapTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        objectMapper.setDefaultTyping(mapTyper);
    }
}