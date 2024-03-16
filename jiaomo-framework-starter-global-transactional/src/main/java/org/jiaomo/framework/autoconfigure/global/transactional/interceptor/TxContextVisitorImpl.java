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
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxBase;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.TxContextVisitor;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxStatusEnum;
import org.jiaomo.framework.commons.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ClassUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
public class TxContextVisitorImpl implements TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> {
    // 本地 commit 执行计时器
    private final Timer commitTimer = MetricsAdapter.timer("TxContextVisitor.commit");
    // 本地 rollback 执行计时器
    private final Timer rollbackTimer = MetricsAdapter.timer("TxContextVisitor.rollback");

    // autonomousTransactional 应用程序 commit 方法执行计时器(23-24或26-27)
    private final Timer autonomousTransactionalServiceCommitTimer = MetricsAdapter.timer("autonomousTransactionalService.commit");
    // autonomousTransactional 应用程序 rollback 方法执行计时器
    private final Timer autonomousTransactionalServiceRollbackTimer = MetricsAdapter.timer("autonomousTransactionalService.rollback");

    private final GlobalTransactionalService globalTransactionalService;
    private final Supplier<ThreadPoolExecutor> globalTransactionalThreadPoolExecutorSupplier;

    public TxContextVisitorImpl(GlobalTransactionalService globalTransactionalService) {
        this.globalTransactionalService = globalTransactionalService;
        this.globalTransactionalThreadPoolExecutorSupplier = this.globalTransactionalService::getGlobalTransactionalThreadPoolExecutor;
    }

    private void makeParentAsCurrent(TxContext txContext) {
        if (txContext.getParent() == null) {
            TxContextHolder.removeCurrent();
        } else {
            TxContextHolder.setCurrent(txContext.getParent());
        }
    }

    private void updateTxStatusOfTxContext(TxStatusEnum txStatusEnum,TxContext txContext) {
        txContext.setTxStatus(txStatusEnum);
        globalTransactionalService.updateTxStatusArgumentsContextByTxCode(txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),
                txContext.getTxStatus(),null,null,txContext.getTxCode());
    }

    @Override
    public void rollback(TxContext txContext) {
        if (txContext == null || TxStatusEnum.NO_TRANSACTIONAL.equals(txContext.getTxStatus()))
            return;

        log.info("GlobalTransactional name:{} txCode:{} will rollback",txContext.getGlobal().getName(),txContext.getTxCode());
        updateTxStatusOfTxContext(TxStatusEnum.ROLLING_BACK,txContext);

        List<TxBase<TxContext>> localChildren = txContext.getChildren();
        if (!globalTransactionalService.isSpeedMode() || localChildren.isEmpty())
            txContext.setChildren(globalTransactionalService.loadChildren(
                    txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),txContext.getRootTxCode(),txContext.getTxCode()));
        for (int idx=txContext.getChildren().size()-1;idx>=0;idx--) {
            TxBase<TxContext> txBase = adjustChild(idx,txContext,localChildren);

            if (txBase.getTxStatus().equals(TxStatusEnum.ROLLED_BACK))
                continue;

            if (txBase instanceof TxContext.AutonomousContext) {
                TxContext.AutonomousContext autonomousContext = (TxContext.AutonomousContext) txBase;
                if (autonomousContext.getTargetObject() == null)
                    globalTransactionalService.rollback(txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),autonomousContext);
                else
                    rollback(txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),autonomousContext);
            }
        }

        updateTxStatusOfTxContext(TxStatusEnum.ROLLED_BACK,txContext);
        makeParentAsCurrent(txContext);
    }

    @Override
    public void rollback(String globalApplicationName,String globalUnitCode,@Nonnull TxContext.AutonomousContext autonomousContext) {
        Timer.Context timerContext = MetricsAdapter.time(rollbackTimer);
        try {
            rollback0(globalApplicationName,globalUnitCode,autonomousContext);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }
    private void rollback0(String globalApplicationName,String globalUnitCode,@Nonnull TxContext.AutonomousContext autonomousContext) {
        Method method = StringUtils.isBlank(autonomousContext.getAutonomous().getRollbackMethod()) ? null :
                ClassUtils.getMethodIfAvailable(autonomousContext.getAutonomous().getTargetClass(),
                        autonomousContext.getAutonomous().getRollbackMethod(),autonomousContext.getAutonomous().getParameterTypes());
        if (method == null) {
            log.warn("{} not found rollback method:{}", autonomousContext.getAutonomous().getName(),autonomousContext.getAutonomous().getRollbackMethod());
            autonomousContext.setTxStatus(TxStatusEnum.ROLLED_BACK);
            globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
                    autonomousContext.getTxStatus(),null,null,autonomousContext.getTxCode());
        } else {
            TxContextHolder.setCurrent(autonomousContext);

            Runnable runnable = () -> {
                try {
                    if (autonomousContext.getAutonomous().isRollbackAsync())
                        TxContextHolder.setCurrent(autonomousContext);
                    autonomousContext.setTxStatus(TxStatusEnum.ROLLING_BACK);
//                  globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
//                          autonomousContext.getTxStatus(),null,null,autonomousContext.getTxCode());
                    globalTransactionalService.getAutonomousTransactionalListener().onAutonomousTransactionRollback(autonomousContext);
                    Timer.Context timerContext = MetricsAdapter.time(autonomousTransactionalServiceRollbackTimer);
                    try {
                        ClassUtils.getMostSpecificMethod(method, autonomousContext.getAutonomous().getTargetClass())
                                .invoke(autonomousContext.getTargetObject(), autonomousContext.getArguments());
                    } finally {
                        MetricsAdapter.stop(timerContext);
                    }
                    autonomousContext.setTxStatus(TxStatusEnum.ROLLED_BACK);
                    globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
                            autonomousContext.getTxStatus(),autonomousContext.getArguments(),autonomousContext.getContext(),autonomousContext.getTxCode());
                    globalTransactionalService.getAutonomousTransactionalListener().onAutonomousTransactionRollbackComplete(autonomousContext);
                } catch (Throwable e) {
                    autonomousContext.setTxStatus(TxStatusEnum.ROLL_BACK_FAILED);
                    globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
                            autonomousContext.getTxStatus(),autonomousContext.getArguments(),autonomousContext.getContext(),autonomousContext.getTxCode());
                    log.error("rollback exception",e);

                    globalTransactionalService.getAutonomousTransactionalListener().onAutonomousTransactionRollbackException(autonomousContext,e);
                } finally {
                    if (autonomousContext.getAutonomous().isRollbackAsync())
                        TxContextHolder.removeCurrent();
                }
            };

            if (autonomousContext.getAutonomous().isRollbackAsync()) {
                globalTransactionalThreadPoolExecutorSupplier.get().submit(runnable);
            } else {
                runnable.run();
            }
            TxContextHolder.setCurrent(TxContextHolder.getCurrent().getParent());
        }
    }

    private TxBase<TxContext> adjustChild(int idx,TxContext txContext,List<TxBase<TxContext>> localChildren) {
        TxBase<TxContext> txBase = txContext.getChildren().get(idx);
        if (!globalTransactionalService.isSpeedMode() || localChildren.isEmpty()) {
            TxBase<TxContext> localChild = findLocalChild(txBase.getTxCode(), localChildren);
            if (localChild == null) {
                txBase.setParent(txContext);
            } else {
                txBase = localChild;
                txContext.getChildren().set(idx, txBase);
            }
        }
        return txBase;
    }

    private TxBase<TxContext> findLocalChild(String txCode,@Nonnull List<TxBase<TxContext>> localChildren) {
        for (TxBase<TxContext> txBase : localChildren) {
            if (txBase.getTxCode().equals(txCode)) {
                return txBase;
            }
        }
        return null;
    }

    @Override
    public void commit(TxContext txContext) {
        if (txContext == null || TxStatusEnum.NO_TRANSACTIONAL.equals(txContext.getTxStatus()))
            return;

        updateTxStatusOfTxContext(TxStatusEnum.COMMITTING,txContext);

        List<TxBase<TxContext>> localChildren = txContext.getChildren();
        if (!globalTransactionalService.isSpeedMode() || localChildren.isEmpty())
            txContext.setChildren(globalTransactionalService.loadChildren(
                    txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),txContext.getRootTxCode(),txContext.getTxCode()));
        for (int idx=0;idx<txContext.getChildren().size();idx++) {
            TxBase<TxContext> txBase = adjustChild(idx,txContext,localChildren);

            if (txBase.getTxStatus().equals(TxStatusEnum.COMMITTED))
                continue;

            if (txBase instanceof TxContext.AutonomousContext) {
                TxContext.AutonomousContext autonomousContext = (TxContext.AutonomousContext) txBase;
                if (autonomousContext.getTargetObject() == null && StringUtils.isNotBlank(autonomousContext.getAutonomous().getCommitMethod()))
                    globalTransactionalService.commit(txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),autonomousContext);
                else
                    commit(txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),autonomousContext);
            }
        }

        updateTxStatusOfTxContext(TxStatusEnum.COMMITTED,txContext);
        makeParentAsCurrent(txContext);
    }

    @Override
    public void commit(String globalApplicationName,String globalUnitCode,@Nonnull TxContext.AutonomousContext autonomousContext) {
        Timer.Context timerContext = MetricsAdapter.time(commitTimer);
        try {
            commit0(globalApplicationName,globalUnitCode,autonomousContext);
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }
    private void commit0(String globalApplicationName,String globalUnitCode,@Nonnull TxContext.AutonomousContext autonomousContext) {
        Method method = StringUtils.isBlank(autonomousContext.getAutonomous().getCommitMethod()) ? null :
                ClassUtils.getMethodIfAvailable(autonomousContext.getAutonomous().getTargetClass(),
                        autonomousContext.getAutonomous().getCommitMethod(), autonomousContext.getAutonomous().getParameterTypes());
        if (method == null) {
            log.info("{} not found commit method:{}", autonomousContext.getAutonomous().getName(),autonomousContext.getAutonomous().getCommitMethod());
            autonomousContext.setTxStatus(TxStatusEnum.COMMITTED);
            globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
                    autonomousContext.getTxStatus(),null,null,autonomousContext.getTxCode());
        } else {
            TxContextHolder.setCurrent(autonomousContext);

            Runnable runnable = () -> {
                try {
                    if (autonomousContext.getAutonomous().isCommitAsync())
                        TxContextHolder.setCurrent(autonomousContext);
                    autonomousContext.setTxStatus(TxStatusEnum.COMMITTING);
//                  globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
//                          autonomousContext.getTxStatus(),null,null,autonomousContext.getTxCode());
                    globalTransactionalService.getAutonomousTransactionalListener().onAutonomousTransactionCommit(autonomousContext);
                    Timer.Context timerContext = MetricsAdapter.time(autonomousTransactionalServiceCommitTimer);
                    try {
                        ClassUtils.getMostSpecificMethod(method, autonomousContext.getAutonomous().getTargetClass())
                                .invoke(autonomousContext.getTargetObject(), autonomousContext.getArguments());
                    } finally {
                        MetricsAdapter.stop(timerContext);
                    }
                    autonomousContext.setTxStatus(TxStatusEnum.COMMITTED);
                    globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
                            autonomousContext.getTxStatus(),autonomousContext.getArguments(),autonomousContext.getContext(),autonomousContext.getTxCode());
                    globalTransactionalService.getAutonomousTransactionalListener().onAutonomousTransactionCommitComplete(autonomousContext);
                } catch (Throwable e) {
                    autonomousContext.setTxStatus(TxStatusEnum.COMMIT_FAILED);
                    globalTransactionalService.updateTxStatusArgumentsContextByTxCode(globalApplicationName,globalUnitCode,
                            autonomousContext.getTxStatus(),autonomousContext.getArguments(),autonomousContext.getContext(),autonomousContext.getTxCode());
                    log.error("commit exception", e);

                    globalTransactionalService.getAutonomousTransactionalListener().onAutonomousTransactionCommitException(autonomousContext,e);
                } finally {
                    if (autonomousContext.getAutonomous().isCommitAsync())
                        TxContextHolder.removeCurrent();
                }
            };

            if (autonomousContext.getAutonomous().isCommitAsync()) {
                globalTransactionalThreadPoolExecutorSupplier.get().submit(runnable);
            } else {
                runnable.run();
            }
            TxContextHolder.setCurrent(TxContextHolder.getCurrent().getParent());
        }
    }

    @Override
    public void checkTimeout(@Nonnull TxContext txContextCurrent) {
        if (txContextCurrent.getGlobal().getTimeoutMills() > 0) {
            if (txContextCurrent.getCreated() == null) {
                log.warn(String.format("GlobalTransactional name:%s txCode:%s timeoutMills:%d created is null",
                        txContextCurrent.getGlobal().getName(),txContextCurrent.getTxCode(),txContextCurrent.getGlobal().getTimeoutMills()));
                return;
            }

            Date now = globalTransactionalService.selectNow(txContextCurrent.getGlobal().getApplicationName(),txContextCurrent.getUnitCode());
            if (now.getTime() - txContextCurrent.getCreated().getTime() > txContextCurrent.getGlobal().getTimeoutMills()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                String msg = String.format("GlobalTransactional name:%s txCode:%s timeoutMills:%d created:%s now:%s timout",
                        txContextCurrent.getGlobal().getName(),txContextCurrent.getTxCode(),txContextCurrent.getGlobal().getTimeoutMills(),
                        sdf.format(txContextCurrent.getCreated()),sdf.format(now));
                log.error(msg);
                throw new BusinessException(msg);
            }
        }
    }
}
