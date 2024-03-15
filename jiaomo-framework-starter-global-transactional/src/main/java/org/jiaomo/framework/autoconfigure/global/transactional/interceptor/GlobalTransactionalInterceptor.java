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

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.TxContextVisitor;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxStatusEnum;
import org.jiaomo.framework.global.transactional.annotation.GlobalTransactional;
import org.jiaomo.framework.commons.UUIDCodeGenerator;
import org.jiaomo.framework.commons.dp.builder.Builder;
import org.jiaomo.framework.commons.exception.BusinessException;
import org.jiaomo.framework.commons.function.ThrowingSupplier;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.ClassUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
public class GlobalTransactionalInterceptor implements MethodInterceptor {
    // 开启 GlobalTransactional 计时器(1-4)
    private final Timer beginTimer = MetricsAdapter.timer("GlobalTransactionalInterceptor.begin");

    @Autowired
    @Qualifier("uuidCodeGeneratorTxCode")
    private UUIDCodeGenerator uuidCodeGeneratorTxCode;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GlobalTransactionalService globalTransactionalService;
    @Autowired
    private TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> txContextVisitor;

    TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> getTxContextVisitor() {
        return this.txContextVisitor;
    }
    GlobalTransactionalService getGlobalTransactionalService() {
        return this.globalTransactionalService;
    }

    @Nullable
    TxContext begin(TxContext.Global global) {
        Timer.Context timerContext = MetricsAdapter.time(beginTimer);
        try {
            return begin0(global);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }
    @Nullable
    private TxContext begin0(TxContext.Global global) {
        globalTransactionalService.checkCurrent();
        TxContext txContext = Builder.of(TxContext::new)
                .with(TxContext::setTxCode, uuidCodeGeneratorTxCode.obtainCode())
                .with(TxContext::setTxStatus, TxStatusEnum.TRANSACTING)
                .with(TxContext::setUnitCode, globalTransactionalService.obtainUnitCode())
                .with(TxContext::setGlobal, global)
                .build();
        if (!needCreateGlobalTransactional(txContext)) {
            return null;
        }

        if (TxContextHolder.getCurrent() == null) {
            txContext.setRootTxCode(txContext.getTxCode());
            TxContextHolder.setCurrent(txContext);
        } else if (TxContextHolder.getCurrent() instanceof TxContext) {
            ((TxContext) TxContextHolder.getCurrent()).acceptCheckTimeout(txContextVisitor);

            txContext.setParent((TxContext)TxContextHolder.getCurrent());
            txContext.setParentTxCode(txContext.getParent().getTxCode());
            txContext.setRootTxCode(txContext.getParent().getRootTxCode());
            txContext.getParent().getChildren().add(txContext);
            TxContextHolder.setCurrent(txContext);
        } else {
            throw new BusinessException("GlobalTransactional define error");
        }
        if (log.isDebugEnabled()) {
            log.debug("txContext:{}", ThrowingSupplier.get(() ->
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(TxContextHolder.getCurrent())));
        }
        globalTransactionalService.save(txContext);
        globalTransactionalService.getGlobalTransactionalListener().onGlobalTransactionalBegin(txContext);
        return txContext;
    }

    private boolean needCreateGlobalTransactional(@Nonnull TxContext txContext) {
        Objects.requireNonNull(txContext);
        TxContext txContextCurrent = (TxContext)TxContextHolder.getCurrent();
        switch (txContext.getGlobal().getPropagation()) {
            case REQUIRED://支持当前事务，如果当前没有事务，就新建一个事务。
                if (txContextCurrent == null || TxStatusEnum.NO_TRANSACTIONAL.equals(txContextCurrent.getTxStatus())) {
                    txContext.setTxStatus(TxStatusEnum.TRANSACTING);
                    return true;
                } else {
                    return false;
                }
            case REQUIRES_NEW://新建事务，如果当前存在事务，把当前事务挂起。
            case NESTED://如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则进行与PROPAGATION_REQUIRED类似的操作。
                txContext.setTxStatus(TxStatusEnum.TRANSACTING);
                return true;
            case SUPPORTS://支持当前事务，如果当前没有事务，就以非事务方式执行。
                return false;
            case MANDATORY://支持当前事务，如果当前没有事务，就抛出异常。
                if (txContextCurrent == null || TxStatusEnum.NO_TRANSACTIONAL.equals(txContextCurrent.getTxStatus())) {
                    String msg = String.format("GlobalTransactional name:%s Propagation.%s txContextCurrent txStatus:%s",
                            txContext.getGlobal().getName(), txContext.getGlobal().getPropagation(),
                            Optional.ofNullable(txContextCurrent).map(TxContext::getTxStatus).orElse(null));
                    log.error(msg);
                    throw new BusinessException(msg);
                } else {
                    return false;
                }
            case NOT_SUPPORTED://以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。
                if (txContextCurrent == null || TxStatusEnum.NO_TRANSACTIONAL.equals(txContextCurrent.getTxStatus())) {
                    return false;
                } else {
                    txContext.setTxStatus(TxStatusEnum.NO_TRANSACTIONAL);
                    return true;
                }
            case NEVER://以非事务方式执行，如果当前存在事务，则抛出异常。
                if (txContextCurrent == null || TxStatusEnum.NO_TRANSACTIONAL.equals(txContextCurrent.getTxStatus())) {
                    return false;
                } else {
                    String msg = String.format("GlobalTransactional name:%s Propagation.%s current has globalTransactional:%s",
                            txContext.getGlobal().getName(), txContext.getGlobal().getPropagation(),txContextCurrent.getTxCode());
                    log.error(msg);
                    throw new BusinessException(msg);
                }
            default:
                return false;
        }
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Class<?> targetClass = methodInvocation.getThis() == null ? null : AopUtils.getTargetClass(methodInvocation.getThis());
        log.debug("GlobalTransactionalInterceptor targetClass name:{} method name:{}",targetClass == null ? null : targetClass.getName(),methodInvocation.getMethod().getName());

        if (targetClass == null || ClassUtils.getMostSpecificMethod(methodInvocation.getMethod(),targetClass).getDeclaringClass().equals(Object.class)) {
            return methodInvocation.proceed();
        }

        final GlobalTransactional globalTransactional = getAnnotation(methodInvocation.getMethod(),targetClass,GlobalTransactional.class);
        if (log.isDebugEnabled()) {
            log.debug("GlobalTransactional timeoutMills:{}", globalTransactional.timeoutMills());
            log.debug("GlobalTransactional name:{}", globalTransactional.name());
            log.debug("GlobalTransactional rollbackFor:{}", Arrays.stream(globalTransactional.rollbackFor()).map(Class::getName).collect(Collectors.toList()));
            log.debug("GlobalTransactional propagation:{}", globalTransactional.propagation());
        }

        TxContext txContext = begin(new TxContext.Global(globalTransactional,this.globalTransactionalService.obtainApplicationName()));
        if (txContext == null) {
            return methodInvocation.proceed();
        }

        final Object returnObject;
        try {
            returnObject = methodInvocation.proceed();
        } catch (Throwable throwable) {
            if (isInstance(globalTransactional.rollbackFor(),throwable)) {
                globalTransactionalService.getGlobalTransactionalListener().onGlobalTransactionalRollback(txContext,throwable);
                txContext.acceptRollback(txContextVisitor);
            } else {
                globalTransactionalService.getGlobalTransactionalListener().onGlobalTransactionalCommit(txContext,throwable);
                txContext.acceptCommit(txContextVisitor);
            }
            throw throwable;
        }

        globalTransactionalService.getGlobalTransactionalListener().onGlobalTransactionalCommit(txContext,null);
        txContext.acceptCommit(txContextVisitor);
        return returnObject;
    }

    private <T extends Annotation> T getAnnotation(Method method, Class<?> targetClass, Class<T> annotationClass) {
        return Optional.ofNullable(method).map(m -> m.getAnnotation(annotationClass))
                .orElse(Optional.ofNullable(targetClass).map(t -> t.getAnnotation(annotationClass)).orElse(null));
    }

    private static boolean isInstance(Class<? extends Throwable>[] classes,Throwable e) {
        if (classes == null || classes.length == 0)
            return e instanceof RuntimeException;

        for (Class<? extends Throwable> clazz : classes) {
            if (clazz.isInstance(e))
                return true;
        }
        return false;
    }
}
