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

import org.jiaomo.framework.autoconfigure.application.context.holder.ApplicationContextHolder;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxBase;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.TxContextVisitor;
import org.springframework.transaction.annotation.Propagation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

public class GlobalTransactionManager {
    private static class TransactionalInterceptorsHolder {
        static volatile GlobalTransactionalInterceptor GTI;
        static volatile AutonomousTransactionalInterceptor ATI;
    }

    private static final Supplier<GlobalTransactionalInterceptor> globalTransactionalInterceptorSupplier = () -> {
        if (TransactionalInterceptorsHolder.GTI == null) {
            synchronized (TransactionalInterceptorsHolder.class) {
                if (TransactionalInterceptorsHolder.GTI == null) {
                    TransactionalInterceptorsHolder.GTI = ApplicationContextHolder.getApplicationContext().getBean(
                            "globalTransactionalInterceptor",GlobalTransactionalInterceptor.class);
                }
            }
        }
        return TransactionalInterceptorsHolder.GTI;
    };

    private static final Supplier<AutonomousTransactionalInterceptor> autonomousTransactionalInterceptorSupplier = () -> {
        if (TransactionalInterceptorsHolder.ATI == null) {
            synchronized (TransactionalInterceptorsHolder.class) {
                if (TransactionalInterceptorsHolder.ATI == null) {
                    TransactionalInterceptorsHolder.ATI = ApplicationContextHolder.getApplicationContext().getBean(
                            "autonomousTransactionalInterceptor",AutonomousTransactionalInterceptor.class);
                }
            }
        }
        return TransactionalInterceptorsHolder.ATI;
    };

    private static final Supplier<TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext>>
            txContextVisitorSupplier = globalTransactionalInterceptorSupplier.get()::getTxContextVisitor;
    private static final Supplier<GlobalTransactionalService>
            globalTransactionalServiceSupplier = globalTransactionalInterceptorSupplier.get()::getGlobalTransactionalService;

    @Nullable
    public static String begin(@Nonnull String name) {
        return begin(name,300000,Propagation.REQUIRED);
    }

    @Nullable
    public static String begin(@Nonnull String name, int timeoutMills,@Nonnull Propagation propagation) {
        return Optional.ofNullable(globalTransactionalInterceptorSupplier.get().begin(
                new TxContext.Global(name,timeoutMills,propagation, globalTransactionalServiceSupplier.get().obtainApplicationName())))
                .map(TxBase::getTxCode).orElse(null);
    }

    public static void commit(String txCode) {
        TxBase<TxContext> txBase = TxContextHolder.getCurrent();
        if (txBase instanceof TxContext && txBase.getTxCode().equals(txCode)) {
            globalTransactionalServiceSupplier.get().getGlobalTransactionalListener().onGlobalTransactionalCommit((TxContext)txBase,null);
            ((TxContext)txBase).acceptCommit(txContextVisitorSupplier.get());
        }
    }

    public static void rollback(String txCode) {
        TxBase<TxContext> txBase = TxContextHolder.getCurrent();
        if (txBase instanceof TxContext && txBase.getTxCode().equals(txCode)) {
            globalTransactionalServiceSupplier.get().getGlobalTransactionalListener().onGlobalTransactionalRollback((TxContext)txBase,null);
            ((TxContext)txBase).acceptRollback(txContextVisitorSupplier.get());
        }
    }

    @SafeVarargs
    public static <T> void autonomousTransactional(Object targetObject,@Nonnull String name, String commitMethod,@Nonnull String rollbackMethod,
                                               Class<?> targetClass, List<Class<?>> parameterTypes,T... arguments) {
        autonomousTransactional(targetObject,name,commitMethod,true,rollbackMethod,false,targetClass,parameterTypes,arguments);
    }

    @SafeVarargs
    public static <T> void autonomousTransactional(Object targetObject,@Nonnull String name, String commitMethod, boolean commitAsync,
                                               @Nonnull String rollbackMethod, boolean rollbackAsync,
                                               Class<?> targetClass, List<Class<?>> parameterTypes,T... arguments) {
        autonomousTransactionalInterceptorSupplier.get().autonomousTransactional(targetObject,
                new TxContext.Autonomous(name,commitMethod,commitAsync,rollbackMethod,rollbackAsync,targetClass,parameterTypes,
                        globalTransactionalServiceSupplier.get().obtainApplicationName(),
                        AutonomousTransactionalInterceptor.obtainBeanNameOfTarget(targetObject,targetClass)),arguments);
    }

    @Nullable
    @SafeVarargs
    public static <T> String startAutonomousTransaction(Object targetObject,@Nonnull String name, String commitMethod, boolean commitAsync,
                                                    @Nonnull String rollbackMethod, boolean rollbackAsync,
                                                    Class<?> targetClass, List<Class<?>> parameterTypes,T... arguments) {
        TxContext.AutonomousContext autonomousContext = autonomousTransactionalInterceptorSupplier.get().autonomousTransactional(targetObject,
                new TxContext.Autonomous(name,commitMethod,commitAsync,rollbackMethod,rollbackAsync,targetClass,parameterTypes,
                        globalTransactionalServiceSupplier.get().obtainApplicationName(),
                        AutonomousTransactionalInterceptor.obtainBeanNameOfTarget(targetObject,targetClass)),arguments);
        if (autonomousContext == null) {
            return null;
        } else {
            TxContextHolder.setCurrent(autonomousContext);
            return autonomousContext.getTxCode();
        }
    }

    @Nullable
    @SafeVarargs
    public static <T> String startAutonomousTransaction(Object targetObject,@Nonnull String name, String commitMethod,
                                                    @Nonnull String rollbackMethod,
                                                    Class<?> targetClass, List<Class<?>> parameterTypes,T... arguments) {
        return startAutonomousTransaction(targetObject,name,commitMethod,true,rollbackMethod,false,targetClass,parameterTypes,arguments);
    }

    @SafeVarargs
    public static <T> void endAutonomousTransaction(String txCode,T... arguments) {
        TxBase<TxContext> txBase = TxContextHolder.getCurrent();
        if (txBase instanceof TxContext.AutonomousContext && txBase.getTxCode().equals(txCode)) {
            globalTransactionalServiceSupplier.get().updateTxStatusArgumentsContextByTxCode(
                    txBase.getParent().getGlobal().getApplicationName(),txBase.getParent().getUnitCode(),
                    null,arguments.length == 0 ? null : arguments,((TxContext.AutonomousContext)txBase).getContext(),txCode);
            globalTransactionalServiceSupplier.get().getAutonomousTransactionalListener().onEndAutonomousTransaction((TxContext.AutonomousContext)txBase);
            TxContextHolder.setCurrent(txBase.getParent());
        }
    }
}
