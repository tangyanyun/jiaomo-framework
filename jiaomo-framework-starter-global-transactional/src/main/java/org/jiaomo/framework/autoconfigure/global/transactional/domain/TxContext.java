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
import org.jiaomo.framework.autoconfigure.global.transactional.interceptor.TxContextVisitorImpl;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.AutonomousContextElement;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.TxContextElement;
import org.jiaomo.framework.commons.function.ThrowingFunction;
import org.jiaomo.framework.global.transactional.annotation.AutonomousTransactional;
import org.jiaomo.framework.global.transactional.annotation.GlobalTransactional;
import lombok.Data;
import org.springframework.transaction.annotation.Propagation;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */


@Data
public class TxContext implements TxBase<TxContext>,Serializable,
        TxContextElement<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> {
    private String txCode;
    private volatile TxStatusEnum txStatus;
    private String parentTxCode;
    private String rootTxCode;
    private String unitCode;
    private Date created;
    @JsonIgnore
    private TxContext parent = null;

    private Global global;

    private List<TxBase<TxContext>> children = new ArrayList<>();

    @Data
    public static class AutonomousContext implements TxBase<TxContext>,Serializable,
            AutonomousContextElement<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> {
        private String txCode;
        private volatile TxStatusEnum txStatus;
        private String parentTxCode;
        private String rootTxCode;
        private String unitCode;
        private Date created;
        @JsonIgnore
        private TxContext parent = null;

        @JsonIgnore
        private Object targetObject = null;
        @JsonIgnore
        private Object[] arguments = null;
        private String argumentsSerial;
        @JsonIgnore
        private Map<String,Object> context = new HashMap<>();
        private String contextSerial;

        private Autonomous autonomous;
    }

    @Data
    public static class Global implements Serializable {
        private int timeoutMills = 60000;
        private String name = "";
        @JsonIgnore
        private Class<? extends Throwable>[] rollbackFor;
        private String[] rollbackForName;
        private Propagation propagation = Propagation.REQUIRED;
        private String applicationName;

        public Global() {
        }

        public Global(GlobalTransactional globalTransactional,String applicationName) {
            this.timeoutMills = globalTransactional.timeoutMills();
            this.name = globalTransactional.name();
            this.propagation = globalTransactional.propagation() == null ? Propagation.REQUIRED : globalTransactional.propagation();
            this.rollbackFor = globalTransactional.rollbackFor();
            this.rollbackForName = Stream.of(this.rollbackFor).map(Class::getName).toArray(String[]::new);
            this.applicationName = applicationName;
        }

        private static final Class<? extends Throwable>[] DEFAULT_ROLLBACK_FOR = ThrowingFunction.sneakyClassCast(new Class[] { RuntimeException.class });
        public Global(String name,int timeoutMills,Propagation propagation,String applicationName) {
            this.timeoutMills = timeoutMills;
            this.name = name;
            this.propagation = propagation == null ? Propagation.REQUIRED : propagation;
            this.rollbackFor = DEFAULT_ROLLBACK_FOR;
            this.rollbackForName = Stream.of(this.rollbackFor).map(Class::getName).toArray(String[]::new);
            this.applicationName = applicationName;
        }
    }

    @Data
    public static class Autonomous implements Serializable {
        private String name;
        private String commitMethod = "";
        private boolean commitAsync = true;
        private String rollbackMethod = "rollback";
        private boolean rollbackAsync = false;
        @JsonIgnore
        private Class<?> targetClass;
        private String targetClassName;
        @JsonIgnore
        private Class<?>[] parameterTypes;
        private String[] parameterTypesName;
        private String applicationName;
        private String beanName;

        public Autonomous() {
        }

        public Autonomous(String name,String commitMethod,boolean commitAsync,String rollbackMethod,boolean rollbackAsync,
                          Class<?> targetClass,List<Class<?>> parameterTypes,String applicationName,String beanName) {
            this.name = name;
            this.commitMethod = commitMethod;
            this.commitAsync = commitAsync;
            this.rollbackMethod = rollbackMethod;
            this.rollbackAsync = rollbackAsync;
            this.targetClass = targetClass;
            this.targetClassName = this.targetClass == null ? null : this.targetClass.getName();
            this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes.toArray(new Class<?>[0]);
            this.parameterTypesName = Stream.of(this.parameterTypes).map(Class::getName).toArray(String[]::new);
            this.applicationName = applicationName;
            this.beanName = beanName;
        }

        public Autonomous(AutonomousTransactional autonomousTransactional,Class<?> targetClass,Class<?>[] parameterTypes,String applicationName,String beanName) {
            this.name = autonomousTransactional.name();
            this.commitMethod = autonomousTransactional.commitMethod();
            this.commitAsync = autonomousTransactional.commitAsync();
            this.rollbackMethod = autonomousTransactional.rollbackMethod();
            this.rollbackAsync = autonomousTransactional.rollbackAsync();
            this.targetClass = targetClass;
            this.targetClassName = this.targetClass == null ? null : this.targetClass.getName();
            this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
            this.parameterTypesName = Stream.of(this.parameterTypes).map(Class::getName).toArray(String[]::new);
            this.applicationName = applicationName;
            this.beanName = beanName;
        }
    }
}
