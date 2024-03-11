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


import com.fasterxml.jackson.databind.ObjectMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.interceptor.GlobalTransactionManager;
import org.jiaomo.framework.autoconfigure.global.transactional.interceptor.TxContextHolder;
import org.jiaomo.framework.commons.function.ThrowingFunction;
import org.jiaomo.framework.global.transactional.annotation.AutonomousTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
public class DemoLocalDomainService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void tryTx(final StringBuilder accountNumber, final BigDecimal amount,final long customerId) {
        log.info("tryTx accountNumber:{} amount:{} customerId:{}",accountNumber,amount,customerId);
        accountNumber.append("-2");

        String txCode = GlobalTransactionManager.startAutonomousTransaction(this,
                "DemoLocalDomainService.tryTx","confirmTx","cancelTx",
                DemoLocalDomainService.class, Arrays.asList(StringBuilder.class, BigDecimal.class,Long.TYPE),
                accountNumber,amount,customerId);
        try {
            jdbcTemplate.update("update tx_demo set amount = amount + 1 where account_number = ?",
                    accountNumber.toString().substring(0,accountNumber.length() - 2));

            //当无分布式事务 或 使用GlobalTransactionManager.autonomousTransactional，TxContextHolder.getContext()为空
            if (TxContextHolder.getContext() != null)
                TxContextHolder.getContext().put("hello", "This is a message from tryTx.");

            if (customerId == 4L) {
                throw new RuntimeException("custom invalid");
            }
        } finally {
            GlobalTransactionManager.endAutonomousTransaction(txCode);
        }
    }

    @Transactional
    public void tryTx52(final StringBuilder accountNumber, final BigDecimal amount,final long customerId) {
        log.info("tryTx52 accountNumber:{} amount:{} customerId:{}",accountNumber,amount,customerId);
        accountNumber.append("-2");

        jdbcTemplate.update("update tx_demo set amount = amount + ? where account_number = ?",BigDecimal.ONE,accountNumber.toString().substring(0,accountNumber.length() - 2));

        if (TxContextHolder.getContext() != null)//当无分布式事务 或 使用GlobalTransactionManager.autonomousTransactional，TxContextHolder.getContext()为空
            TxContextHolder.getContext().put("hello","This is a message from tryTx.");

        GlobalTransactionManager.autonomousTransactional(this,"DemoLocalDomainService.tryTx","confirmTx","cancelTx",
                DemoLocalDomainService.class, Arrays.asList(StringBuilder.class, BigDecimal.class,Long.TYPE),accountNumber,amount,customerId);

        if (customerId == 4L) {
            throw new RuntimeException("custom invalid");
        }
    }

    @AutonomousTransactional(name="DemoLocalDomainService.tryTxWithAnnotation", commitMethod = "confirmTx",rollbackMethod = "cancelTx")
    @Transactional
    public void tryTxWithAnnotation(final StringBuilder accountNumber, final BigDecimal amount,final long customerId) {
        jdbcTemplate.update("update tx_demo set amount = amount + 1 where account_number = ?",accountNumber);
        accountNumber.append("-2");
        if (TxContextHolder.getContext() != null)//当无分布式事务时,TxContextHolder.getContext()为空
            TxContextHolder.getContext().put("hello","This is a message from tryTx.");
        if (customerId == 4L) {
            throw new RuntimeException("custom invalid");
        }
    }

    @Transactional
    public BigDecimal confirmTx(StringBuilder accountNumber, BigDecimal amount,long customerId) {
        String hello = Optional.ofNullable(TxContextHolder.getContext()).map(m -> m.get("hello"))
                .map(ThrowingFunction::<String>sneakyClassCast).orElse("");
        jdbcTemplate.update("update tx_demo set remark = ? where account_number = ?",
                accountNumber.toString() + hello, accountNumber.toString().substring(0,accountNumber.length() - 2));
        return amount;
    }

    @Transactional
    public BigDecimal cancelTx(StringBuilder accountNumber, BigDecimal amount,long customerId) {
        String hello = Optional.ofNullable(TxContextHolder.getContext()).map(m -> m.get("hello")).map(ThrowingFunction::<String>sneakyClassCast).orElse("");
        jdbcTemplate.update("update tx_demo set remark = ? where account_number = ?",
                "cancelTx" + accountNumber.toString() + hello, accountNumber.toString().substring(0,accountNumber.length() - 2));
        return amount;
    }

    @AutonomousTransactional(name="DemoLocalDomainService.tryTxBenchmark", commitMethod = "confirmTxBenchmark",rollbackMethod = "cancelTxBenchmark")
    public void tryTxBenchmark(final StringBuilder accountNumber, final BigDecimal amount,final long customerId) {
    }
    public BigDecimal confirmTxBenchmark(StringBuilder accountNumber, BigDecimal amount,long customerId) {
        return BigDecimal.ZERO;
    }
    public BigDecimal cancelTxBenchmark(StringBuilder accountNumber, BigDecimal amount,long customerId) {
        return BigDecimal.ZERO;
    }

}
