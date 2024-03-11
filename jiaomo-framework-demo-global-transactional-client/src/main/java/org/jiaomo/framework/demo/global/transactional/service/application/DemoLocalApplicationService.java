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

package org.jiaomo.framework.demo.global.transactional.service.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.interceptor.GlobalTransactionManager;
import org.jiaomo.framework.commons.exception.BusinessException;
import org.jiaomo.framework.demo.global.transactional.facade.DemoGlobalTransactionalWithAnnotationFacade;
import org.jiaomo.framework.demo.global.transactional.service.domain.DemoLocalDomainService;
import org.jiaomo.framework.global.transactional.annotation.GlobalTransactional;
import org.jiaomo.framework.demo.global.transactional.facade.DemoGlobalTransactionalFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Service
public class DemoLocalApplicationService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DemoLocalDomainService demoLocalDomainService;
    @Resource
    private DemoGlobalTransactionalFacade demoGlobalTransactionalFacade;
    @Resource
    private DemoGlobalTransactionalWithAnnotationFacade demoGlobalTransactionalWithAnnotationFacade;

    @Autowired
    @Lazy
    private DemoLocalApplicationService demoLocalApplicationService;


    @GlobalTransactional(name="DemoLocalApplicationService.tx01WithAnnotation",
            timeoutMills = 300000,
            rollbackFor = { RuntimeException.class },
            propagation = Propagation.REQUIRED)
    public void tx01WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.tccTryDSWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx02WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx02WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        log.info("====== tx2 ========");
        demoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation0(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
//        demoLocalApplicationService.tx01WithAnnotation(accountNumber,amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx03WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx03WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        log.info("====== tx3 ========");
        demoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
//        demoLocalApplicationService.tx01WithAnnotation(accountNumber,amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx07WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx07WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranASWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx08WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx08WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    public void tx09(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx10WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx10WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx11WithAnnotation",rollbackFor = {BusinessException.class},propagation = Propagation.REQUIRED)
    public void tx11WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx12WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx12WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotationRequiresNew(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx14WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx14WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        try {
            demoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotationRequiresNew(accountNumber, amount, customerId);
        } catch (RuntimeException e) {
            log.info(e.getMessage(),e);
        }
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    //SUPPORTS://支持当前事务，如果当前没有事务，就以非事务方式执行。
    @GlobalTransactional(name="DemoLocalApplicationService.tx16WithAnnotationSupports",propagation = Propagation.SUPPORTS)
    public void tx16WithAnnotationSupports(String accountNumber, BigDecimal amount,long customerId) {
        try {
            demoGlobalTransactionalFacade.appTranASWithAnnotation(accountNumber, amount, customerId);
        } catch (RuntimeException e) {
            log.info(e.getMessage(),e);
        }
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    //MANDATORY://支持当前事务，如果当前没有事务，就抛出异常。
    @GlobalTransactional(name="DemoLocalApplicationService.tx18WithAnnotationMandatory",propagation = Propagation.MANDATORY)
    public void tx18WithAnnotationMandatory(String accountNumber, BigDecimal amount,long customerId) {
        try {
            demoGlobalTransactionalFacade.appTranASWithAnnotation(accountNumber, amount, customerId);
        } catch (RuntimeException e) {
            log.info(e.getMessage(),e);
        }
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx20WithAnnotation",propagation = Propagation.REQUIRED)
    public void tx20WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranMandatory(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx22WithAnnotationNotSupported",propagation = Propagation.NOT_SUPPORTED)
    public void tx22WithAnnotationNotSupported(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranMandatory(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx24WithAnnotationNotSupported",propagation = Propagation.NOT_SUPPORTED)
    public void tx24WithAnnotationNotSupported(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranASWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx26WithAnnotation")
    public void tx26WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranNotSupported(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx28WithAnnotationNever",propagation = Propagation.NEVER)
    public void tx28WithAnnotationNever(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranASWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx30WithAnnotation")
    public void tx30WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranNever(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx32WithAnnotationNever",propagation = Propagation.NEVER)
    public void tx32WithAnnotationNever(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.appTranNever(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }

    public void tx51(String accountNumber, BigDecimal amount,long customerId) {
        String txCode = GlobalTransactionManager.begin("DemoLocalApplicationService.tx51");
        try {
            demoGlobalTransactionalFacade.tccTry(accountNumber, amount, customerId);
            demoLocalDomainService.tryTx(new StringBuilder(accountNumber), amount, customerId);
        } catch (RuntimeException e) {
            GlobalTransactionManager.rollback(txCode);
            throw e;
        } catch (Throwable e) {
            GlobalTransactionManager.commit(txCode);
            throw e;
        }
        GlobalTransactionManager.commit(txCode);
    }

    public void tx52(String accountNumber, BigDecimal amount,long customerId) {
        String txCode = GlobalTransactionManager.begin("DemoLocalApplicationService.tx52");
        try {
            demoGlobalTransactionalFacade.tccTry(accountNumber, amount, customerId);
            demoLocalDomainService.tryTx52(new StringBuilder(accountNumber), amount, customerId);
        } catch (RuntimeException e) {
            GlobalTransactionManager.rollback(txCode);
            throw e;
        } catch (Throwable e) {
            GlobalTransactionManager.commit(txCode);
            throw e;
        }
        GlobalTransactionManager.commit(txCode);
    }

    public void tx53(String accountNumber, BigDecimal amount,long customerId) {
        String txCode = GlobalTransactionManager.begin("DemoLocalApplicationService.tx53");
        try {
            demoGlobalTransactionalFacade.tccTry(accountNumber, amount, customerId);
            demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount, customerId);
        } catch (RuntimeException e) {
            GlobalTransactionManager.rollback(txCode);
            throw e;
        } catch (Throwable e) {
            GlobalTransactionManager.commit(txCode);
            throw e;
        }
        GlobalTransactionManager.commit(txCode);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.tx54WithAnnotation")
    public void tx54WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.tccTry(accountNumber, amount, customerId);
        demoLocalDomainService.tryTx(new StringBuilder(accountNumber), amount, customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.txBenchmark1",
            timeoutMills = 300000,
            rollbackFor = { RuntimeException.class },
            propagation = Propagation.REQUIRED)
    public void txBenchmark1(String accountNumber, BigDecimal amount,long customerId) {
        demoLocalDomainService.tryTxBenchmark(new StringBuilder(accountNumber), amount,customerId);
    }

    @GlobalTransactional(name="DemoLocalApplicationService.txBenchmark2",
            timeoutMills = 300000,
            rollbackFor = { RuntimeException.class },
            propagation = Propagation.REQUIRED)
    public void txBenchmark2(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.tccTryBenchmark(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxBenchmark(new StringBuilder(accountNumber), amount,customerId);
    }
}
