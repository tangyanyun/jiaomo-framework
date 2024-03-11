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

public enum TxStatusEnum {
    NO_TRANSACTIONAL((byte)4,"非事务"),
    TRANSACTING((byte)10,"交易中"),
    TRANSACTED((byte)12,"已交易"),
    TRANSACT_FAILED((byte)14,"交易失败"),
    COMMITTING((byte)20,"提交中"),
    COMMITTED((byte)22,"已提交"),
    COMMIT_FAILED((byte)24,"提交失败"),
    ROLLING_BACK((byte)30,"回滚中"),
    ROLLED_BACK((byte)32,"已回滚"),
    ROLL_BACK_FAILED((byte)34,"回滚失败");

    private byte code;
    private String description;

    TxStatusEnum(byte code, String description) {
        this.code = code;
        this.description = description;
    }

    public byte getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }

    public static TxStatusEnum codeOf(byte code) {
        for (TxStatusEnum enum1 : values()) {
            if (enum1.getCode() == code) return enum1;
        }
        return null;
    }
    public static TxStatusEnum descriptionOf(String description) {
        for (TxStatusEnum enum1 : values()) {
            if (enum1.getDescription().equals(description)) return enum1;
        }
        return null;
    }
    public static TxStatusEnum ordinalOf(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length)
            return null;
        else
            return values()[ordinal];
    }
}
