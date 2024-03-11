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
import org.jiaomo.framework.autoconfigure.global.transactional.listener.AutonomousTransactionalListener;
import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.TxContextVisitor;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxStatusEnum;
import org.jiaomo.framework.autoconfigure.global.transactional.spring.beans.GetterBeanNameAware;
import org.jiaomo.framework.commons.jackson.JsonJacksonSerializer;
import org.jiaomo.framework.global.transactional.annotation.AutonomousTransactional;
import org.jiaomo.framework.commons.UUIDCodeGenerator;
import org.jiaomo.framework.commons.function.ThrowingSupplier;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */


@Slf4j
public class AutonomousTransactionalInterceptor implements MethodInterceptor {
    // autonomousTransactional计时器(7-11或15-18),不含应用程序自身执行时长
    private static final Timer autonomousTransactionalTimer = MetricsAdapter.timer("AutonomousTransactionalInterceptor.autonomousTransactional");
    // 应用程序(tcc的try方法或saga正向交易)执行计时器(11-12或18-19)
    private final Timer autonomousTransactionalServiceTranTimer = MetricsAdapter.timer("autonomousTransactionalService.tran");

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    @Qualifier("uuidCodeGeneratorTxCode")
    private UUIDCodeGenerator uuidCodeGeneratorTxCode;
    @Autowired
    private GlobalTransactionalService globalTransactionalService;
    @Autowired
    private TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> txContextVisitor;

    @Nullable
    TxContext.AutonomousContext autonomousTransactional(Object targetObject, @Nonnull TxContext.Autonomous autonomous, Object[] arguments) {
        Timer.Context timerContext = MetricsAdapter.time(autonomousTransactionalTimer);
        try {
            return autonomousTransactional0(targetObject,autonomous,arguments);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }
    @Nullable
    private TxContext.AutonomousContext autonomousTransactional0(Object targetObject,TxContext.Autonomous autonomous,Object[] arguments) {
        globalTransactionalService.checkCurrent();
        if (!(TxContextHolder.getCurrent() instanceof TxContext) ||
                TxStatusEnum.NO_TRANSACTIONAL.equals(TxContextHolder.getCurrent().getTxStatus())) {
            return null;
        }
        TxContext txContextCurrent = (TxContext) TxContextHolder.getCurrent();
        txContextCurrent.acceptCheckTimeout(txContextVisitor);

        TxContext.AutonomousContext autonomousContext = new TxContext.AutonomousContext();
        autonomousContext.setTxCode(uuidCodeGeneratorTxCode.obtainCode());
        autonomousContext.setTxStatus(TxStatusEnum.TRANSACTING);
        autonomousContext.setRootTxCode(txContextCurrent.getRootTxCode());
        autonomousContext.setParentTxCode(txContextCurrent.getTxCode());
        autonomousContext.setUnitCode(globalTransactionalService.obtainUnitCode());
        autonomousContext.setTargetObject(targetObject);
        autonomousContext.setArguments(arguments);
        autonomousContext.setArgumentsSerial(JsonJacksonSerializer.INSTANCE.serializeAsString(arguments));
        autonomousContext.setContextSerial(JsonJacksonSerializer.INSTANCE.serializeAsString(autonomousContext.getContext()));
        autonomousContext.setAutonomous(autonomous);
        autonomousContext.setParent(txContextCurrent);
        if (log.isTraceEnabled()) {
            log.trace("autonomousTransactional autonomousContext:{}",ThrowingSupplier.get(() ->
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(autonomousContext)));
        }
        txContextCurrent.getChildren().add(autonomousContext);
        globalTransactionalService.save(txContextCurrent.getGlobal().getApplicationName(), txContextCurrent.getUnitCode(), autonomousContext);
        globalTransactionalService.getAutonomousTransactionalListener().onStartAutonomousTransaction(autonomousContext);
        return autonomousContext;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Class<?> targetClass = methodInvocation.getThis() == null ? null : methodInvocation.getMethod().getDeclaringClass();
        if (log.isDebugEnabled()) {
            log.debug("AutonomousTransactionalInterceptor targetClass name:{} method name:{}",
                    targetClass == null ? null : targetClass.getName(), methodInvocation.getMethod().getName());
        }
        if (targetClass == null) {
            return methodInvocation.proceed();
        }

        AutonomousTransactional autonomousTransactional = methodInvocation.getMethod().getAnnotation(AutonomousTransactional.class);
        Object[] arguments = methodInvocation.getArguments();
        if (log.isDebugEnabled()) {
            log.debug("autonomousTransactional name:{}", autonomousTransactional.name());
            log.debug("autonomousTransactional commitMethod:{}", autonomousTransactional.commitMethod());
            log.debug("autonomousTransactional commitAsync:{}", autonomousTransactional.commitAsync());
            log.debug("autonomousTransactional rollbackMethod:{}", autonomousTransactional.rollbackMethod());
            log.debug("autonomousTransactional rollbackAsync:{}", autonomousTransactional.rollbackAsync());
            log.debug("autonomousTransactional arguments:{}",ThrowingSupplier.get(() -> objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arguments)));
        }

        TxContext.AutonomousContext autonomousContext = autonomousTransactional(methodInvocation.getThis(),
                new TxContext.Autonomous(autonomousTransactional,targetClass,methodInvocation.getMethod().getParameterTypes(),
                        this.globalTransactionalService.obtainApplicationName(),obtainBeanNameOfTarget(methodInvocation.getThis(),targetClass)),arguments);
        if (autonomousContext == null) {
            return methodInvocation.proceed();
        }
        TxContextHolder.setCurrent(autonomousContext);

        Timer.Context timerContext = MetricsAdapter.time(autonomousTransactionalServiceTranTimer);
        try {
            Object returnObject = methodInvocation.proceed();
            autonomousContext.setTxStatus(TxStatusEnum.TRANSACTED);
            return returnObject;
        } catch (Throwable throwable) {
            autonomousContext.setTxStatus(TxStatusEnum.TRANSACT_FAILED);
            throw throwable;
        } finally {
            MetricsAdapter.stop(timerContext);
            globalTransactionalService.updateTxStatusArgumentsContextByTxCode(autonomousContext.getParent().getGlobal().getApplicationName(),
                    autonomousContext.getParent().getUnitCode(),
                    autonomousContext.getTxStatus(), arguments, autonomousContext.getContext(), autonomousContext.getTxCode());
            globalTransactionalService.getAutonomousTransactionalListener().onEndAutonomousTransaction(autonomousContext);
            TxContextHolder.setCurrent(autonomousContext.getParent());
        }
    }

    static String obtainBeanNameOfTarget(Object targetObject,Class<?> targetClass) {
        if (targetObject instanceof GetterBeanNameAware)
            return ((GetterBeanNameAware)targetObject).getBeanName();
        else
            return null;
/*
        if (targetObject instanceof BeanNameAware) {
            Field field = ThrowingSupplier.getWithoutThrowing(() -> targetClass.getDeclaredField("beanName"));
            if (field == null)
                field = ThrowingSupplier.getWithoutThrowing(() -> targetClass.getField("beanName"));
            if (field == null) {
                Method method = ClassUtils.getMethodIfAvailable(targetClass,"getBeanName");
                if (method == null) {
                    return null;
                } else {
                    return ThrowingFunction.sneakyClassCast(ThrowingSupplier.get(() ->
                            ClassUtils.getMostSpecificMethod(method, targetClass).invoke(targetObject)));
                }
            } else {
                field.setAccessible(true);
                return ThrowingFunction.sneakyClassCast(ThrowingFunction.<Field,Object,IllegalAccessException>toFunction(f ->
                        f.get(targetObject)).apply(field));
            }
        } else {
            return null;
        }
*/
    }
}
