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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jiaomo.framework.commons.function.ThrowingFunction;

import java.util.Date;
import java.util.Optional;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

public interface TxBase<P extends TxBase<P>> {
    String getTxCode();
    void setTxCode(String txCode);

    TxStatusEnum getTxStatus();
    void setTxStatus(TxStatusEnum txStatus);

    @JsonIgnore
    P getParent();
    void setParent(P parent);

    String getParentTxCode();
    void setParentTxCode(String parentTxCode);
    String getRootTxCode();
    void setRootTxCode(String rootTxCode);

    String getUnitCode();
    void setUnitCode(String unitCode);

    Date getCreated();
    void setCreated(Date created);

    @JsonIgnore
    default P getRoot() {
        TxBase<P> txBase = this;
        while (txBase.getParent() != null) {
            txBase = txBase.getParent();
        }
        return ThrowingFunction.sneakyClassCast(txBase);
    }
}