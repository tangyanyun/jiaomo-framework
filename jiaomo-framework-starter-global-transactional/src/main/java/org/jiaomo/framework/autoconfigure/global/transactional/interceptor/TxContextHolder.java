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

package org.jiaomo.framework.autoconfigure.global.transactional.interceptor;

//import com.alibaba.ttl.TransmittableThreadLocal;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxBase;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;

import java.util.Map;
import java.util.Optional;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

public class TxContextHolder {
//  private static final ThreadLocal<TxBase<TxContext>> current = new TransmittableThreadLocal<>();
//  private static final ThreadLocal<String> currentTxCode = new TransmittableThreadLocal<>();
    private static final ThreadLocal<TxBase<TxContext>> current = new ThreadLocal<>();
    private static final ThreadLocal<String> currentTxCode = new ThreadLocal<>();

    static TxBase<TxContext> getCurrent() {
        return current.get();
    }

    static void setCurrent(TxBase<TxContext> txBase) {
        current.set(txBase);
    }

    static void removeCurrent() {
        current.remove();
    }

    static String getCurrentTxCode() {
        return currentTxCode.get();
    }
    static void setCurrentTxCode(String txCode) {
        currentTxCode.set(txCode);
    }
    static void removeCurrentTxCode() {
        currentTxCode.remove();
    }

    public static Map<String,Object> getContext() {
        return Optional.ofNullable(getCurrent()).filter(txBase -> txBase instanceof TxContext.AutonomousContext)
                .map(txBase -> ((TxContext.AutonomousContext)txBase).getContext()).orElse(null);
    }
}