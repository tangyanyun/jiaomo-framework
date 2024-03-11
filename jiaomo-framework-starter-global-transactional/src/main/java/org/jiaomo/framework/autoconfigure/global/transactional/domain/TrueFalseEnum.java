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

package org.jiaomo.framework.autoconfigure.global.transactional.domain;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

public enum TrueFalseEnum {
    FALSE((byte)0,false,"no","否"),
    TRUE((byte)1,true,"yes","是");

    private byte code;
    private boolean value;
    private String description;
    private String descriptionCn;

    TrueFalseEnum(byte code, boolean value, String description,String descriptionCn) {
        this.code = code;
        this.value = value;
        this.description = description;
        this.descriptionCn = descriptionCn;
    }

    public byte getCode() {
        return this.code;
    }
    public boolean getValue() {
        return this.value;
    }
    public String getDescription() {
        return this.description;
    }
    public String getDescriptionCn() {
        return this.descriptionCn;
    }

    public static TrueFalseEnum codeOf(byte code) {
        for (TrueFalseEnum enum1 : values()) {
            if (enum1.getCode() == code) return enum1;
        }
        return null;
    }
    public static TrueFalseEnum descriptionOf(String description) {
        for (TrueFalseEnum enum1 : values()) {
            if (enum1.getDescription().equalsIgnoreCase(description)) return enum1;
        }
        return null;
    }
    public static TrueFalseEnum descriptionCnOf(String descriptionCn) {
        for (TrueFalseEnum enum1 : values()) {
            if (enum1.getDescriptionCn().equals(descriptionCn)) return enum1;
        }
        return null;
    }
    public static TrueFalseEnum ordinalOf(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length)
            return null;
        else
            return values()[ordinal];
    }

    public static TrueFalseEnum booleanOf(boolean value) {
        if (value)
            return TRUE;
        else
            return FALSE;
    }
}
