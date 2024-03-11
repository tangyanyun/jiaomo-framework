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

import org.jiaomo.framework.commons.function.ThrowingFunction;

import javax.annotation.Nonnull;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

public interface AutonomousContextElement<V extends TxContextVisitor<V,T,A>,T extends TxContextElement<V,T,A>,A extends AutonomousContextElement<V,T,A>> {

    default void acceptRollback(String globalApplicationName,String globalUnitCode,@Nonnull TxContextVisitor<V,T,A> v) {
        v.rollback(globalApplicationName,globalUnitCode,ThrowingFunction.<A>sneakyClassCast(this));
    }

    default void acceptCommit(String globalApplicationName,String globalUnitCode,@Nonnull TxContextVisitor<V,T,A> v) {
        v.commit(globalApplicationName,globalUnitCode,ThrowingFunction.<A>sneakyClassCast(this));
    }
}