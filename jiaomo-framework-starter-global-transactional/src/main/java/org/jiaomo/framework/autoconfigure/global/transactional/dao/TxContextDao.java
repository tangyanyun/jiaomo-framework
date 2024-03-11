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

package org.jiaomo.framework.autoconfigure.global.transactional.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jiaomo.framework.autoconfigure.application.context.holder.ApplicationContextHolder;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TrueFalseEnum;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxBase;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxStatusEnum;
import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.mapper.TxAutonomousMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.mapper.TxContextMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.mapper.TxGlobalMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxAutonomousPo;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxContextLoadPo;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxContextPo;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxGlobalPo;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.TxAutonomousService;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.TxContextService;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.TxGlobalService;
import org.jiaomo.framework.commons.jackson.JsonJacksonSerializer;
import org.jiaomo.framework.commons.dp.builder.Builder;
import org.jiaomo.framework.commons.function.ThrowingFunction;
import org.jiaomo.framework.commons.function.ThrowingRunnable;
import org.jiaomo.framework.commons.function.ThrowingSupplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
public class TxContextDao {
    // 本地 selectNow 执行计时器
    private final Timer selectNowTimer = MetricsAdapter.timer("TxContextDao.selectNow");
    // 本地 save 执行计时器
    private final Timer saveTimer = MetricsAdapter.timer("TxContextDao.save");
    // 本地 updateTxStatusArgumentsContextByTxCode 执行计时器
    private final Timer updateTxStatusArgumentsContextByTxCodeTimer = MetricsAdapter.timer("TxContextDao.updateTxStatusArgumentsContextByTxCode");
    // 本地 load 执行计时器
    private final Timer loadTimer = MetricsAdapter.timer("TxContextDao.load");
    // 本地 loadChildren 执行计时器
    private final Timer loadChildrenTimer = MetricsAdapter.timer("TxContextDao.loadChildren");

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TxContextMapper txContextMapper;
    @Autowired
    private TxContextService txContextService;
    @Autowired
    private TxGlobalService txGlobalService;
    @Autowired
    private TxGlobalMapper txGlobalMapper;
    @Autowired
    private TxAutonomousMapper txAutonomousMapper;
    @Autowired
    private TxAutonomousService txAutonomousService;

    public Date selectNow() {
        Timer.Context timerContext = MetricsAdapter.time(selectNowTimer);
        try {
            return txContextMapper.selectNow();
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(@Nonnull TxContext txContext) {
        Timer.Context timerContext = MetricsAdapter.time(saveTimer);
        try {
            save0(txContext);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }
    private void save0(@Nonnull TxContext txContext) {
        Objects.requireNonNull(txContext);
        TxGlobalPo txGlobalPo = Builder.of(TxGlobalPo::new)
                .with(TxGlobalPo::setName, txContext.getGlobal().getName())
                .with(TxGlobalPo::setTimeoutMills, txContext.getGlobal().getTimeoutMills())
                .with(TxGlobalPo::setRollbackForName, ThrowingSupplier.get(() ->
                        objectMapper.writeValueAsString(txContext.getGlobal().getRollbackForName())))
                .with(TxGlobalPo::setPropagation, txContext.getGlobal().getPropagation().value())
                .with(TxGlobalPo::setApplicationName, txContext.getGlobal().getApplicationName())
                .build();
        TxGlobalPo record = txGlobalService.getOne(new QueryWrapper<TxGlobalPo>().lambda().eq(TxGlobalPo::getName,txGlobalPo.getName()));
        if (record == null) {
            ThrowingRunnable.<DataAccessException>runWithoutThrowing(() -> txGlobalMapper.insertOrUpdate(txGlobalPo));
        } else if (!txGlobalPo.getTimeoutMills().equals(record.getTimeoutMills()) ||
                !StringUtils.trimToEmpty(txGlobalPo.getRollbackForName()).equals(StringUtils.trimToEmpty(record.getRollbackForName())) ||
                !txGlobalPo.getPropagation().equals(record.getPropagation()) ||
                !StringUtils.trimToEmpty(txGlobalPo.getApplicationName()).equals(StringUtils.trimToEmpty(record.getApplicationName()))) {
            txGlobalService.update(txGlobalPo,new UpdateWrapper<TxGlobalPo>().lambda().eq(TxGlobalPo::getName, txGlobalPo.getName()));
        }

        TxContextPo txContextPo = Builder.of(TxContextPo::new)
                .with(TxContextPo::setTxCode, txContext.getTxCode())
                .with(TxContextPo::setRootTxCode, txContext.getRootTxCode())
                .with(TxContextPo::setParentTxCode, txContext.getParentTxCode())
                .with(TxContextPo::setTxStatus, txContext.getTxStatus().getCode())
                .with(TxContextPo::setUnitCode, txContext.getUnitCode())
                .with(TxContextPo::setGlobalName, txContext.getGlobal().getName())
                .build();
        txContextService.save(txContextPo);
        txContext.setCreated(txContextMapper.selectCreatedByTxCode(txContext.getTxCode()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(@Nonnull TxContext.AutonomousContext autonomousContext) {
        Timer.Context timerContext = MetricsAdapter.time(saveTimer);
        try {
            save0(autonomousContext);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }
    private void save0(@Nonnull TxContext.AutonomousContext autonomousContext) {
        Objects.requireNonNull(autonomousContext);
        TxAutonomousPo txAutonomousPo = Builder.of(TxAutonomousPo::new)
                .with(TxAutonomousPo::setName,autonomousContext.getAutonomous().getName())
                .with(TxAutonomousPo::setCommitMethod,autonomousContext.getAutonomous().getCommitMethod())
                .with(TxAutonomousPo::setRollbackMethod,autonomousContext.getAutonomous().getRollbackMethod())
                .with(TxAutonomousPo::setCommitAsync,TrueFalseEnum.booleanOf(autonomousContext.getAutonomous().isCommitAsync()).getCode())
                .with(TxAutonomousPo::setRollbackAsync,TrueFalseEnum.booleanOf(autonomousContext.getAutonomous().isRollbackAsync()).getCode())
                .with(TxAutonomousPo::setTargetClassName,autonomousContext.getAutonomous().getTargetClassName())
                .with(TxAutonomousPo::setParameterTypesName,ThrowingSupplier.get(() ->
                        objectMapper.writeValueAsString(autonomousContext.getAutonomous().getParameterTypesName())))
                .with(TxAutonomousPo::setApplicationName,autonomousContext.getAutonomous().getApplicationName())
                .with(TxAutonomousPo::setBeanName,autonomousContext.getAutonomous().getBeanName())
                .build();
        TxAutonomousPo record = txAutonomousService.getOne(new QueryWrapper<TxAutonomousPo>().lambda().eq(TxAutonomousPo::getName,txAutonomousPo.getName()));
        if (record == null) {
            ThrowingRunnable.<DataAccessException>runWithoutThrowing(() -> txAutonomousMapper.insertOrUpdate(txAutonomousPo));
        } else if (!txAutonomousPo.getName().equals(record.getName()) ||
                !StringUtils.trimToEmpty(txAutonomousPo.getCommitMethod()).equals(StringUtils.trimToEmpty(record.getCommitMethod())) ||
                !StringUtils.trimToEmpty(txAutonomousPo.getRollbackMethod()).equals(StringUtils.trimToEmpty(record.getRollbackMethod())) ||
                !txAutonomousPo.getCommitAsync().equals(record.getCommitAsync()) ||
                !txAutonomousPo.getRollbackAsync().equals(record.getRollbackAsync()) ||
                !StringUtils.trimToEmpty(txAutonomousPo.getTargetClassName()).equals(StringUtils.trimToEmpty(record.getTargetClassName())) ||
                !StringUtils.trimToEmpty(txAutonomousPo.getParameterTypesName()).equals(StringUtils.trimToEmpty(record.getParameterTypesName())) ||
                !StringUtils.trimToEmpty(txAutonomousPo.getApplicationName()).equals(StringUtils.trimToEmpty(record.getApplicationName())) ||
                !StringUtils.trimToEmpty(txAutonomousPo.getBeanName()).equals(StringUtils.trimToEmpty(record.getBeanName()))) {
            txAutonomousService.update(txAutonomousPo,new UpdateWrapper<TxAutonomousPo>().lambda().eq(TxAutonomousPo::getName,txAutonomousPo.getName()));
        }

        TxContextPo txContextPo = Builder.of(TxContextPo::new)
                .with(TxContextPo::setTxCode,autonomousContext.getTxCode())
                .with(TxContextPo::setRootTxCode,autonomousContext.getRootTxCode())
                .with(TxContextPo::setParentTxCode,autonomousContext.getParentTxCode())
                .with(TxContextPo::setTxStatus,autonomousContext.getTxStatus().getCode())
                .with(TxContextPo::setUnitCode,autonomousContext.getUnitCode())
                .with(TxContextPo::setAutonomousName,autonomousContext.getAutonomous().getName())
                .with(TxContextPo::setArgumentsSerial,autonomousContext.getArgumentsSerial())
                .with(TxContextPo::setContextSerial,autonomousContext.getContextSerial())
                .build();
        txContextService.save(txContextPo);
        autonomousContext.setCreated(txContextMapper.selectCreatedByTxCode(autonomousContext.getTxCode()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTxStatusArgumentsContextByTxCode(Byte txStatus,String argumentsSerial,String contextSerial,@Nonnull String txCode) {
        Timer.Context timerContext = MetricsAdapter.time(updateTxStatusArgumentsContextByTxCodeTimer);
        try {
            updateTxStatusArgumentsContextByTxCode0(txStatus,argumentsSerial,contextSerial,txCode);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }
    private void updateTxStatusArgumentsContextByTxCode0(Byte txStatus,String argumentsSerial,String contextSerial,@Nonnull String txCode) {
        assert StringUtils.isNotBlank(txCode);
        LambdaUpdateWrapper<TxContextPo> lambdaUpdateWrapper = new UpdateWrapper<TxContextPo>().lambda();
        if (txStatus != null)
            lambdaUpdateWrapper.set(TxContextPo::getTxStatus, txStatus);
        if (StringUtils.isNotBlank(argumentsSerial))
            lambdaUpdateWrapper.set(TxContextPo::getArgumentsSerial, argumentsSerial);
        if (StringUtils.isNotBlank(contextSerial))
            lambdaUpdateWrapper.set(TxContextPo::getContextSerial, contextSerial);
        lambdaUpdateWrapper.eq(TxContextPo::getTxCode, txCode);
        txContextService.update(lambdaUpdateWrapper);
    }

    private static Propagation propagationValueOf(int value) {
        for (Propagation p : Propagation.values()) {
            if (value == p.value())
                return p;
        }
        return null;
    }

    public static Class<?> classForName(String name) {
        switch (name) {
            case "boolean":
                return Boolean.TYPE;
            case "char":
                return Character.TYPE;
            case "byte":
                return Byte.TYPE;
            case "short":
                return Short.TYPE;
            case "int":
                return Integer.TYPE;
            case "long":
                return Long.TYPE;
            case "float":
                return Float.TYPE;
            case "double":
                return Double.TYPE;
            case "void":
                return Void.TYPE;
            default:
                return ThrowingSupplier.get(() -> Class.forName(name));
        }
    }

    private Function<TxContextLoadPo,TxBase<TxContext>> functionLoad = m -> {
        Objects.requireNonNull(m);
        if (log.isDebugEnabled())
            log.debug("functionLoad:{}",ThrowingSupplier.get(() -> objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(m)));

        if (StringUtils.isNotBlank(m.getGlobalName())) {
            return Builder.of(TxContext::new)
                    .with(TxContext::setTxCode,m.getTxCode())
                    .with(TxContext::setTxStatus,TxStatusEnum.codeOf(m.getTxStatus()))
                    .with(TxContext::setRootTxCode,m.getRootTxCode())
                    .with(TxContext::setParentTxCode,m.getParentTxCode())
                    .with(TxContext::setUnitCode,m.getUnitCode())
                    .with(TxContext::setCreated,m.getCreated())
                    .with(TxContext::setGlobal,Builder.of(TxContext.Global::new)
                            .with(TxContext.Global::setTimeoutMills,m.getTimeoutMills())
                            .with(TxContext.Global::setName,m.getGlobalName())
                            .with(TxContext.Global::setRollbackForName,ThrowingSupplier.<String[],JsonProcessingException>get(() ->
                                    objectMapper.readValue(m.getRollbackForName(),new TypeReference<String[]>() {})))
                            .with(TxContext.Global::setPropagation,propagationValueOf(m.getPropagation()))
                            .with(TxContext.Global::setApplicationName,m.getGlobalApplicationName())
                            .build())
                    .build();
        } else if (StringUtils.isNotBlank(m.getAutonomousName())) {
            return Builder.of(TxContext.AutonomousContext::new)
                    .with(TxContext.AutonomousContext::setTxCode,m.getTxCode())
                    .with(TxContext.AutonomousContext::setTxStatus,TxStatusEnum.codeOf(m.getTxStatus()))
                    .with(TxContext.AutonomousContext::setRootTxCode,m.getRootTxCode())
                    .with(TxContext.AutonomousContext::setParentTxCode,m.getParentTxCode())
                    .with(TxContext.AutonomousContext::setUnitCode,m.getUnitCode())
                    .with(TxContext.AutonomousContext::setCreated,m.getCreated())
                    .with(TxContext.AutonomousContext::setArgumentsSerial,m.getArgumentsSerial())
                    .with(TxContext.AutonomousContext::setContextSerial,m.getContextSerial())
                    .with(TxContext.AutonomousContext::setAutonomous,Builder.of(TxContext.Autonomous::new)
                            .with(TxContext.Autonomous::setName,m.getAutonomousName())
                            .with(TxContext.Autonomous::setCommitMethod,m.getCommitMethod())
                            .with(TxContext.Autonomous::setCommitAsync,Optional.ofNullable(
                                    TrueFalseEnum.codeOf(m.getCommitAsync())).map(TrueFalseEnum::getValue).orElse(true))
                            .with(TxContext.Autonomous::setRollbackMethod,m.getRollbackMethod())
                            .with(TxContext.Autonomous::setRollbackAsync,Optional.ofNullable(
                                    TrueFalseEnum.codeOf(m.getRollbackAsync())).map(TrueFalseEnum::getValue).orElse(false))
                            .with(TxContext.Autonomous::setTargetClassName,m.getTargetClassName())
                            .with(TxContext.Autonomous::setParameterTypesName,ThrowingSupplier.get(() ->
                                    objectMapper.readValue(m.getParameterTypesName(), new TypeReference<String[]>() {})))
                            .with(TxContext.Autonomous::setApplicationName,m.getAutonomousApplicationName())
                            .with(TxContext.Autonomous::setBeanName,m.getBeanName())
                            .build())
                    .build();
        } else {
            return null;
        }
    };

    @Nullable
    public TxBase<TxContext> load(@Nonnull String txCode) {
        Timer.Context timerContext = MetricsAdapter.time(loadTimer);
        try {
            if (log.isDebugEnabled()) {
                log.debug("begin of load txCode:{}", txCode);
            }
            return txContextMapper.load(txCode,null,null).stream().map(functionLoad).findFirst().orElse(null);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }

    @Nonnull
    public List<TxBase<TxContext>> loadChildren(String rootTxCode,String parentTxCode) {
        Timer.Context timerContext = MetricsAdapter.time(loadChildrenTimer);
        try {
            return txContextMapper.load(null,rootTxCode,parentTxCode).stream().map(functionLoad).collect(Collectors.toList());
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }

    public static <T extends TxBase<TxContext>> T loadClasses(T txBase) {
        if (txBase instanceof TxContext) {
            TxContext txContext = (TxContext) txBase;
            txContext.getGlobal().setRollbackFor(ThrowingFunction.sneakyClassCast(
                    Stream.of(txContext.getGlobal().getRollbackForName()).map(TxContextDao::classForName).toArray(Class[]::new)));
            return txBase;
        } else if (txBase instanceof TxContext.AutonomousContext) {
            TxContext.AutonomousContext autonomousContext = (TxContext.AutonomousContext) txBase;

            autonomousContext.setArguments(JsonJacksonSerializer.INSTANCE.deserialize(autonomousContext.getArgumentsSerial(),Object[].class));
            autonomousContext.setContext(JsonJacksonSerializer.INSTANCE.deserialize(autonomousContext.getContextSerial(), new TypeReference<Map<String, Object>>() {}));

            autonomousContext.getAutonomous().setTargetClass(TxContextDao.classForName(autonomousContext.getAutonomous().getTargetClassName()));
            autonomousContext.getAutonomous().setParameterTypes(Stream.of(autonomousContext.getAutonomous().getParameterTypesName()).map(TxContextDao::classForName).toArray(Class[]::new));

            if (StringUtils.isBlank(autonomousContext.getAutonomous().getBeanName())) {
                autonomousContext.setTargetObject(ApplicationContextHolder.getApplicationContext().getBean(
                        autonomousContext.getAutonomous().getTargetClass()));
            } else {
                autonomousContext.setTargetObject(ApplicationContextHolder.getApplicationContext().getBean(
                        autonomousContext.getAutonomous().getBeanName(), autonomousContext.getAutonomous().getTargetClass()));
            }
            return txBase;
        } else {
            return txBase;
        }
    }

    public List<TxBase<TxContext>> findByGlobalApplicationNameAndUnitCodeAndStatusesAndRecoverTime(String globalApplicationName,String unitCode,
                                                                                                   Collection<TxStatusEnum> statuses,Date recoverTime) {
        return txContextMapper.findByGlobalApplicationNameAndUnitCodeAndStatusesAndRecoverTime(globalApplicationName,unitCode,
                        Optional.ofNullable(statuses).map(l -> l.stream().map(TxStatusEnum::getCode).collect(Collectors.toList())).orElse(null),recoverTime)
                .stream().map(functionLoad).collect(Collectors.toList());
    }
}
