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

import org.jiaomo.framework.autoconfigure.global.transactional.dao.TxContextDao;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxBase;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxStatusEnum;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.TxContextVisitor;
import org.jiaomo.framework.commons.ApiResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
public class GlobalTransactionalController {
    public static final String PATH_PREFIX = "/globalTransactionalRemoteService/X_3MB79WNwpDa5OFNZXsGzY8";

    private final TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> txContextVisitor;
    private final GlobalTransactionalService globalTransactionalService;
    private final Supplier<TxContextDao> txContextDaoSupplier;

    public GlobalTransactionalController(@Nonnull TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> txContextVisitor,
                                         @Nonnull GlobalTransactionalService globalTransactionalService) {
        this.txContextVisitor = txContextVisitor;
        this.globalTransactionalService = globalTransactionalService;
        this.txContextDaoSupplier = this.globalTransactionalService::getTxContextDao;
    }

    @PostMapping(PATH_PREFIX + "/selectNow")
    @ResponseBody
    public Date selectNow() {
        return txContextDaoSupplier.get().selectNow();
    }

    @PostMapping(PATH_PREFIX + "/saveTxContext")
    @ResponseBody
    public void saveTxContext(@RequestBody TxContext txContext) {
        txContextDaoSupplier.get().save(txContext);
    }

    @PostMapping(PATH_PREFIX + "/saveAutonomousContext")
    @ResponseBody
    public void saveAutonomousContext(@RequestBody TxContext.AutonomousContext autonomousContext) {
        txContextDaoSupplier.get().save(autonomousContext);
    }

    @Data
    static class UpdaterTxContextDto {
        private Byte txStatus;
        private String argumentsSerial;
        private String contextSerial;
        private String txCode;
    }

    @PostMapping(PATH_PREFIX + "/updateTxStatusArgumentsContextByTxCode")
    @ResponseBody
    public void updateTxStatusArgumentsContextByTxCode(@RequestBody UpdaterTxContextDto dto) {
        txContextDaoSupplier.get().updateTxStatusArgumentsContextByTxCode(
                dto.getTxStatus(),dto.getArgumentsSerial(),dto.getContextSerial(),dto.getTxCode());
    }

    @PostMapping(PATH_PREFIX + "/load")
    @ResponseBody
    public TxBase<TxContext> load(@RequestParam("txCode") String txCode) {
        return txContextDaoSupplier.get().load(txCode);
    }

    @PostMapping(PATH_PREFIX + "/loadChildren")
    @ResponseBody
    public List<TxBase<TxContext>> loadChildren(@RequestParam("rootTxCode") String rootTxCode, @RequestParam("parentTxCode") String parentTxCode) {
        return txContextDaoSupplier.get().loadChildren(rootTxCode,parentTxCode);
    }

    @PostMapping(PATH_PREFIX + "/commit")
    @ResponseBody
    public ApiResult<Void> commit(@RequestParam("globalApplicationName") String globalApplicationName,@RequestParam("globalUnitCode") String globalUnitCode,
                                  @RequestBody TxContext.AutonomousContext autonomousContext) {
        try {
            TxContextDao.loadClasses(autonomousContext).acceptCommit(globalApplicationName,globalUnitCode,txContextVisitor);
        } finally {
            TxContextHolder.removeCurrent();
        }
        return ApiResult.SUCCESS;
    }

    @PostMapping(PATH_PREFIX + "/rollback")
    @ResponseBody
    public ApiResult<Void> rollback(@RequestParam("globalApplicationName") String globalApplicationName,@RequestParam("globalUnitCode") String globalUnitCode,
                                    @RequestBody TxContext.AutonomousContext autonomousContext) {
        try {
            TxContextDao.loadClasses(autonomousContext).acceptRollback(globalApplicationName,globalUnitCode,txContextVisitor);
        } finally {
            TxContextHolder.removeCurrent();
        }
        return ApiResult.SUCCESS;
    }

    @PostMapping(PATH_PREFIX + "/recover")
    @ResponseBody
    public ApiResult<Void> recover(@RequestParam(name = "recoverTime",required = false) Date recoverTime) {
        globalTransactionalService.getGlobalTransactionalThreadPoolExecutor().submit(() -> {
            List<TxBase<TxContext>> list = txContextDaoSupplier.get().findByGlobalApplicationNameAndUnitCodeAndStatusesAndRecoverTime(
                    globalTransactionalService.obtainApplicationName(),globalTransactionalService.obtainUnitCode(),
                    Arrays.asList(TxStatusEnum.TRANSACTING,TxStatusEnum.COMMITTING,TxStatusEnum.ROLLING_BACK),
                    recoverTime == null ? txContextDaoSupplier.get().selectNow() : recoverTime);
            for (TxBase<TxContext> txBase : list) {
                if (txBase instanceof TxContext) {
                    TxContext txContext = (TxContext) txBase;
                    switch (txContext.getTxStatus()) {
                        case TRANSACTING:
                        case ROLLING_BACK:
                            txContext.acceptRollback(txContextVisitor);
                            break;
                        case COMMITTING:
                            txContext.acceptCommit(txContextVisitor);
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        return ApiResult.SUCCESS;
    }
}
