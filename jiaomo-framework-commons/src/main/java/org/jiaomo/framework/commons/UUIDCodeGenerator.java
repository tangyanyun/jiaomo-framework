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

package org.jiaomo.framework.commons;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public class UUIDCodeGenerator {
    private short prefixAndKind;

    public UUIDCodeGenerator(String base64PrefixAndKind) {
        this.prefixAndKind = ByteBuffer.wrap(Base64.getUrlDecoder().decode(base64PrefixAndKind.substring(0,2) + "AA")).getShort();
    }

    public UUIDCodeGenerator(char base64PrefixCharacter, char base64KindCharacter) {
        this.prefixAndKind = ByteBuffer.wrap(Base64.getUrlDecoder().decode(String.valueOf(base64PrefixCharacter) + base64KindCharacter + "AA")).getShort();
    }

    public String getBase64PrefixAndKind() {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[2]);
        byteBuffer.putShort(prefixAndKind);
        return Base64.getUrlEncoder().encodeToString(byteBuffer.array()).substring(0,2);
    }

    public String obtainCode() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[18]);
        byteBuffer.putShort((short)(prefixAndKind | ThreadLocalRandom.current().nextInt(16)));
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());

        return Base64.getUrlEncoder().encodeToString(byteBuffer.array());
    }

    public static UUID codeOf(String code) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(code));
        byteBuffer.getShort();
        return new UUID(byteBuffer.getLong(),byteBuffer.getLong());
    }

    public static void main(String[] args) {
        System.out.println(String.format("%04x",(short)(ByteBuffer.wrap(Base64.getUrlDecoder().decode("__".substring(0,2) + "AA")).getShort() | (7 & 0xF))));
        System.out.println(new UUIDCodeGenerator("__").obtainCode());
        System.out.println(new UUIDCodeGenerator('C','_').obtainCode());
        System.out.println(new UUIDCodeGenerator('C','_').getBase64PrefixAndKind());
    }
}
