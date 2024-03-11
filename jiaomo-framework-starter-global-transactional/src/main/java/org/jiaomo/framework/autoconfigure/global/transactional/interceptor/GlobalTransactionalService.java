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
import org.jiaomo.framework.autoconfigure.global.transactional.dao.TxContextDao;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxBase;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxStatusEnum;
import org.jiaomo.framework.autoconfigure.global.transactional.listener.AutonomousTransactionalListener;
import org.jiaomo.framework.autoconfigure.global.transactional.listener.GlobalTransactionalListener;
import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import org.jiaomo.framework.commons.function.LambdaUtils;
import org.jiaomo.framework.commons.jackson.JsonJacksonSerializer;
import org.jiaomo.framework.commons.ApiResult;
import org.jiaomo.framework.commons.function.ThrowingSupplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
public class GlobalTransactionalService {
    // 远程 selectNow 执行计时器(9-10)
    private final Timer selectNowTimer = MetricsAdapter.timer("GlobalTransactionalService.selectNow");
    // 远程 save 执行计时器(9-10)
    private final Timer saveTimer = MetricsAdapter.timer("GlobalTransactionalService.save");
    // 远程 updateTxStatusArgumentsContextByTxCode 执行计时器(9-10)
    private final Timer updateTxStatusArgumentsContextByTxCodeTimer = MetricsAdapter.timer("GlobalTransactionalService.updateTxStatusArgumentsContextByTxCode");
    // 远程 load 执行计时器(9-10)
    private final Timer loadTimer = MetricsAdapter.timer("GlobalTransactionalService.load");
    // 远程 loadChildren 执行计时器(9-10)
    private final Timer loadChildrenTimer = MetricsAdapter.timer("GlobalTransactionalService.loadChildren");
    // 远程 commit 执行计时器(22-25)
    private final Timer commitTimer = MetricsAdapter.timer("GlobalTransactionalService.commit");
    // 远程 rollback 执行计时器
    private final Timer rollbackTimer = MetricsAdapter.timer("GlobalTransactionalService.rollback");

    @Value("${spring.application.name:}")
    private String springApplicationName;
    @Value("${unit.code:}")
    private String unitCode;
    @Getter
    @Value("${global-transactional.speed-mode:false}")
    private boolean speedMode;
    @Autowired
    @Qualifier("globalTransactionalRestTemplate")
    private RestTemplate restTemplate;
    @Getter
    @Autowired
    @Qualifier("globalTransactionalThreadPoolExecutor")
    private ThreadPoolExecutor globalTransactionalThreadPoolExecutor;
    @Autowired
    private ObjectMapper objectMapper;
    @Getter
    @Autowired
    private TxContextDao txContextDao;
    @Getter
    @Autowired
    private GlobalTransactionalListener globalTransactionalListener;
    @Getter
    @Autowired
    private AutonomousTransactionalListener autonomousTransactionalListener;

    public String obtainApplicationName() {
        return this.springApplicationName;
    }
    public String obtainUnitCode() {
        return this.unitCode;
    }

    void checkCurrent() {
        String applicationNameUnitCodeTxCode = TxContextHolder.getCurrentTxCode();
        if (TxContextHolder.getCurrent() == null && StringUtils.isNotBlank(applicationNameUnitCodeTxCode)) {
            int idx = applicationNameUnitCodeTxCode.indexOf(GlobalTransactionalWebMvcHandlerInterceptor.SEPARATOR);
            String applicationName = applicationNameUnitCodeTxCode.substring(0,idx);
            int idy = applicationNameUnitCodeTxCode.indexOf(GlobalTransactionalWebMvcHandlerInterceptor.SEPARATOR,idx + 1);
            String unitCode = applicationNameUnitCodeTxCode.substring(idx + 1,idy);
            String txCode = applicationNameUnitCodeTxCode.substring(idy + 1);
            TxBase<TxContext> txContext = load(applicationName,unitCode,txCode);
            TxContextHolder.setCurrent(txContext);
            TxContextHolder.removeCurrentTxCode();
            if (log.isTraceEnabled()) {
                log.trace("provider Interceptor txCode:{} txContext:{}",txCode, ThrowingSupplier.get(() ->
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(txContext)));
            }
        }
    }

    public Date selectNow(String globalApplicationName,String globalUnitCode) {
        return execute(globalApplicationName,globalUnitCode,selectNowTimer,
                () -> txContextDao.selectNow(),
                () -> exchange(globalApplicationName,globalUnitCode,"/selectNow?unitCode={1}",
                        HttpMethod.POST,null,new ParameterizedTypeReference<Date>() {}).getBody());
    }

    public void save(@Nonnull TxContext txContext) {
        execute(txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),saveTimer,
                () -> {
                    txContextDao.save(txContext);
                    return null;
                },
                () -> {
                    exchange(txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),"/saveTxContext?unitCode={1}",
                            HttpMethod.POST,new HttpEntity<>(txContext),new ParameterizedTypeReference<Void>() {});
                    return null;
                });
    }
    public void save(String globalApplicationName,String globalUnitCode,@Nonnull TxContext.AutonomousContext autonomousContext) {
        execute(globalApplicationName,globalUnitCode,saveTimer,
                () -> {
                    txContextDao.save(autonomousContext);
                    return null;
                },
                () -> {
                    exchange(globalApplicationName,globalUnitCode,"/saveAutonomousContext?unitCode={1}",
                            HttpMethod.POST,new HttpEntity<>(autonomousContext),new ParameterizedTypeReference<Void>() {});
                    return null;
                });
    }

    public void updateTxStatusArgumentsContextByTxCode(String globalApplicationName,String globalUnitCode,TxStatusEnum txStatusEnum, Object[] arguments,
                                                       Map<String,Object> context,@Nonnull String txCode) {
        execute(globalApplicationName,globalUnitCode,updateTxStatusArgumentsContextByTxCodeTimer,
                () -> {
                    txContextDao.updateTxStatusArgumentsContextByTxCode(
                            txStatusEnum == null ? null : txStatusEnum.getCode(),
                            arguments == null ? null : JsonJacksonSerializer.INSTANCE.serializeAsString(arguments),
                            context == null ? null : JsonJacksonSerializer.INSTANCE.serializeAsString(context),
                            txCode);
                    return null;
                },
                () -> {
                    GlobalTransactionalController.UpdaterTxContextDto dto = new GlobalTransactionalController.UpdaterTxContextDto();
                    dto.setTxStatus(txStatusEnum == null ? null : txStatusEnum.getCode());
                    dto.setArgumentsSerial(arguments == null ? null : JsonJacksonSerializer.INSTANCE.serializeAsString(arguments));
                    dto.setContextSerial(context == null ? null : JsonJacksonSerializer.INSTANCE.serializeAsString(context));
                    dto.setTxCode(txCode);
                    exchange(globalApplicationName,globalUnitCode,"/updateTxStatusArgumentsContextByTxCode?unitCode={1}",
                            HttpMethod.POST,new HttpEntity<>(dto), new ParameterizedTypeReference<Void>() {});
                    return null;
                });
    }

    @Nullable
    public TxBase<TxContext> load(String globalApplicationName,String globalUnitCode,String txCode) {
        return execute(globalApplicationName,globalUnitCode,loadTimer,
                () -> txContextDao.load(txCode),
                () -> exchange(globalApplicationName,globalUnitCode,"/load?unitCode={1}&txCode={2}",HttpMethod.POST,null,
                        new ParameterizedTypeReference<TxContext>() {},txCode).getBody());
    }

    @Nonnull
    public List<TxBase<TxContext>> loadChildren(String globalApplicationName,String globalUnitCode,String rootTxCode, String parentTxCode) {
        return execute(globalApplicationName,globalUnitCode,loadChildrenTimer,
                () -> txContextDao.loadChildren(rootTxCode,parentTxCode),
                () -> Optional.ofNullable(exchange(globalApplicationName,globalUnitCode,"/loadChildren?unitCode={1}&rootTxCode={2}&parentTxCode={3}",
                                HttpMethod.POST,null,new ParameterizedTypeReference<List<TxBase<TxContext>>>() {},rootTxCode,parentTxCode).getBody())
                        .orElseGet(ArrayList::new));
    }

    protected boolean checkLocal(String globalApplicationName,String globalUnitCode) {
        return isSpeedMode() || (this.obtainApplicationName().equals(globalApplicationName) && this.obtainUnitCode().equals(globalUnitCode));
    }

    protected <T> T execute(String globalApplicationName,String globalUnitCode,Timer timer,Supplier<T> localSupplier,Supplier<T> remoteSupplier) {
        if (checkLocal(globalApplicationName,globalUnitCode))
            return localSupplier.get();
        else {
            Timer.Context timerContext = MetricsAdapter.time(timer);
            try {
                return remoteSupplier.get();
            } finally {
                MetricsAdapter.stop(timerContext);
            }
        }
    }

    public void commit(String globalApplicationName,String globalUnitCode,@Nonnull TxContext.AutonomousContext autonomousContext) {
        execute(commitTimer,() -> {
            ApiResult<Void> apiResult = exchange(autonomousContext.getAutonomous().getApplicationName(),autonomousContext.getUnitCode(),
                    "/commit?unitCode={1}&globalApplicationName={2}&globalUnitCode={3}",
                    HttpMethod.POST,new HttpEntity<>(autonomousContext),new ParameterizedTypeReference<ApiResult<Void>>() {},
                    globalApplicationName,globalUnitCode).getBody();
            log.debug("remote commit result:{}",apiResult == null ? null : apiResult.isSuccess());
        });
    }

    public void rollback(String globalApplicationName,String globalUnitCode,@Nonnull TxContext.AutonomousContext autonomousContext) {
        execute(rollbackTimer,() -> {
            ApiResult<Void> apiResult = exchange(autonomousContext.getAutonomous().getApplicationName(),autonomousContext.getUnitCode(),
                    "/rollback?unitCode={1}&globalApplicationName={2}&globalUnitCode={3}",
                    HttpMethod.POST,new HttpEntity<>(autonomousContext),new ParameterizedTypeReference<ApiResult<Void>>() {},
                    globalApplicationName,globalUnitCode).getBody();
            log.debug("remote rollback result:{}",apiResult == null ? null : apiResult.isSuccess());
        });
    }

    protected void execute(Timer timer,Runnable runnable) {
        Timer.Context timerContext = MetricsAdapter.time(timer);
        try {
            runnable.run();
        } finally {
            MetricsAdapter.stop(timerContext);
        }
    }

    protected <T> ResponseEntity<T> exchange(String globalApplicationName, String globalUnitCode, String url,
                                             HttpMethod httpMethod, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Object... uriVariables) {
        return restTemplate.exchange("http://" + globalApplicationName + GlobalTransactionalController.PATH_PREFIX + url,
                httpMethod,requestEntity,responseType,
                LambdaUtils.get(() -> {
                    Object[] oa = new Object[1 + uriVariables.length];
                    oa[0] = globalUnitCode;
                    System.arraycopy(uriVariables,0,oa,1,uriVariables.length);
                    return oa;
                }));
    }
}
