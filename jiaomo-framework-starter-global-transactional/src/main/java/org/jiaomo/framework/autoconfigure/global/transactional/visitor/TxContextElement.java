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

package org.jiaomo.framework.autoconfigure.global.transactional.visitor;


import com.codahale.metrics.Timer;
import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import org.jiaomo.framework.commons.function.ThrowingFunction;

import javax.annotation.Nonnull;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

public interface TxContextElement<V extends TxContextVisitor<V,T,A>,T extends TxContextElement<V,T,A>,A extends AutonomousContextElement<V,T,A>> {
    // GlobalTransactional commit 阶段执行计时器(20-28)
    Timer commitTimer = MetricsAdapter.timer("GlobalTransactionalInterceptor.commit");
    // GlobalTransactional rollback 阶段执行计时器
    Timer rollbackTimer = MetricsAdapter.timer("GlobalTransactionalInterceptor.rollback");

    default void acceptRollback(@Nonnull TxContextVisitor<V,T,A> v) {
        Timer.Context timerContext = MetricsAdapter.time(rollbackTimer);
        try {
            v.rollback(ThrowingFunction.<T>sneakyClassCast(this));
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }

    default void acceptCommit(@Nonnull TxContextVisitor<V,T,A> v) {
        Timer.Context timerContext = MetricsAdapter.time(commitTimer);
        try {
            v.commit(ThrowingFunction.<T>sneakyClassCast(this));
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }

    default void acceptCheckTimeout(TxContextVisitor<V,T,A> v) {
        v.checkTimeout(ThrowingFunction.<T>sneakyClassCast(this));
    }
}
