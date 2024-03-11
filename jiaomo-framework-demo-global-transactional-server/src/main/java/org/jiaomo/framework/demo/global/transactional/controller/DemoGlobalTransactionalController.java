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

package org.jiaomo.framework.demo.global.transactional.controller;

import org.jiaomo.framework.commons.ApiResult;
import org.jiaomo.framework.demo.global.transactional.facade.DemoGlobalTransactionalFacade;
import org.jiaomo.framework.demo.global.transactional.facade.DemoGlobalTransactionalWithAnnotationFacade;
import org.jiaomo.framework.demo.global.transactional.service.application.DemoGlobalTransactionalApplicationService;
import org.jiaomo.framework.demo.global.transactional.service.domain.DemoGlobalTransactionalDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class DemoGlobalTransactionalController implements DemoGlobalTransactionalFacade, DemoGlobalTransactionalWithAnnotationFacade {
    @Autowired
    private DemoGlobalTransactionalApplicationService demoGlobalTransactionalApplicationService;
    @Autowired
    private DemoGlobalTransactionalDomainService demoGlobalTransactionalDomainService;

    @Override
    public ApiResult<Void> tccTry(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTry(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> tccTryDSWithAnnotation(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTryWithAnnotation(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> sagaTxDSWithAnnotation(String accountNumber,BigDecimal amount,long customerId) {
        demoGlobalTransactionalDomainService.sagaTxWithAnnotation(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> appTranASWithAnnotation(String accountNumber,BigDecimal amount, long customerId) {
        demoGlobalTransactionalApplicationService.appTranWithAnnotation(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> appTranMandatory(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalApplicationService.appTranWithAnnotationMandatory(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> appTranNotSupported(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalApplicationService.appTranWithAnnotationNotSupported(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> appTranNever(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalApplicationService.appTranWithAnnotationNever(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> tccTryWithAnnotation(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTryWithAnnotation(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> tccConfirm(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccConfirm(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> tccCancel(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccCancel(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> sagaTxWithAnnotation(String accountNumber,BigDecimal amount,long customerId) {
        demoGlobalTransactionalDomainService.sagaTxWithAnnotation(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> sagaRollback(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.sagaRollback(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> appTranWithAnnotation(String accountNumber,BigDecimal amount, long customerId) {
        demoGlobalTransactionalApplicationService.appTranWithAnnotation(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> appTranWithAnnotationRequiresNew(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalApplicationService.appTranWithAnnotation(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> tccTryWithAnnotation0(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTry(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> sagaTxWithAnnotation0(String accountNumber,BigDecimal amount,long customerId) {
        demoGlobalTransactionalDomainService.sagaTx(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> appTranWithAnnotation0(String accountNumber,BigDecimal amount, long customerId) {
        demoGlobalTransactionalApplicationService.appTran(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }

    @Override
    public ApiResult<Void> tccTryBenchmark(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTryBenchmark(accountNumber,amount,customerId);
        return ApiResult.SUCCESS;
    }
}