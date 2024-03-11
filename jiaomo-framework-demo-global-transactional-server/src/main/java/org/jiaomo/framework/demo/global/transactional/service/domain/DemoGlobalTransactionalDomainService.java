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

package org.jiaomo.framework.demo.global.transactional.service.domain;

import org.jiaomo.framework.autoconfigure.global.transactional.interceptor.GlobalTransactionManager;
import org.jiaomo.framework.autoconfigure.global.transactional.spring.beans.GetterBeanNameAware;
import org.jiaomo.framework.global.transactional.annotation.AutonomousTransactional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

@Slf4j
@Service
public class DemoGlobalTransactionalDomainService implements GetterBeanNameAware {
    @Getter @Setter
    private String beanName;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void tccTry(String accountNumber, BigDecimal amount, long customerId) {
        jdbcTemplate.update("insert into tx_demo (account_number,amount,customer_id) values (?,?,?)",
                accountNumber,amount,customerId);

        GlobalTransactionManager.autonomousTransactional(this,
                "DemoGlobalTransactionalDomainService.tccTry",
                "tccConfirm", true,
                "tccCancel", false,
                DemoGlobalTransactionalDomainService.class,
                Arrays.asList(String.class,BigDecimal.class,Long.TYPE),
                accountNumber,amount,customerId);
    }

    @Transactional
    @AutonomousTransactional(name="DemoGlobalTransactionalDomainService.tccTryWithAnnotation",
            commitMethod = "tccConfirm",rollbackMethod = "tccCancel")
    public void tccTryWithAnnotation(String accountNumber, BigDecimal amount, long customerId) {
        log.info("DemoGlobalTransactionalService.tccTry =======");
        jdbcTemplate.update("insert into tx_demo (account_number,amount,customer_id,status) values (?,?,?,?)",
                accountNumber,amount,customerId,(byte)0);
    }


    @Transactional
    public void tccConfirm(String accountNumber, BigDecimal amount, long customerId) {
        log.info("DemoGlobalTransactionalService.tccConfirm ======= accountNumber:{} amount:{}",accountNumber,amount);
        jdbcTemplate.update("update tx_demo set status = 1 where account_number = ?",accountNumber);
        log.info("DemoGlobalTransactionalService.tccConfirm ======= status:{}",
                jdbcTemplate.queryForObject("select status from tx_demo where account_number = ?",Byte.class,accountNumber));
    }

    @Transactional
    public void tccCancel(String accountNumber, BigDecimal amount, long customerId) {
        jdbcTemplate.update("update tx_demo set status = 4 where account_number = ?",accountNumber);
        log.info("DemoGlobalTransactionalService.tccCancel =======");
    }

    //////////////////////////////////////////////////////////////////////////

    @Transactional
    public void sagaTx(String accountNumber, BigDecimal amount, long customerId) {
        log.info("DemoGlobalTransactionalService.sagaTx =======");
    }

    @AutonomousTransactional(name="DemoGlobalTransactionalDomainService.sagaTxWithAnnotation",rollbackMethod = "sagaRollback")
    @Transactional
    public void sagaTxWithAnnotation(String accountNumber, BigDecimal amount, long customerId) {
        log.info("DemoGlobalTransactionalService.sagaTxWithAnnotation =======");
        jdbcTemplate.update("update tx_demo set amount = amount + 1 where account_number = ?",accountNumber);
        if (customerId == 5L) {
            throw new RuntimeException("custom invalid");
        }
    }

    @Transactional
    public void sagaRollback(String accountNumber, BigDecimal amount, long customerId) {
        log.info("DemoGlobalTransactionalService.sagaRollback =======");
        jdbcTemplate.update("update tx_demo set amount = amount - 1 where account_number = ?",accountNumber);
    }

    @AutonomousTransactional(name="DemoGlobalTransactionalDomainService.tccTryBenchmark",
            commitMethod = "tccConfirmBenchmark",rollbackMethod = "tccCancelBenchmark")
    public void tccTryBenchmark(String accountNumber, BigDecimal amount, long customerId) {
    }
    public void tccConfirmBenchmark(String accountNumber, BigDecimal amount, long customerId) {
    }
    public void tccCancelBenchmark(String accountNumber, BigDecimal amount, long customerId) {
    }
}
