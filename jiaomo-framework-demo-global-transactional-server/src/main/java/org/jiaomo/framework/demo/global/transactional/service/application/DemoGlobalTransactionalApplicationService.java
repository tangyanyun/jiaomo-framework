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

import org.jiaomo.framework.demo.global.transactional.service.domain.DemoGlobalTransactionalDomainService;
import org.jiaomo.framework.global.transactional.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;

@Service
public class DemoGlobalTransactionalApplicationService {
    @Autowired
    private DemoGlobalTransactionalDomainService demoGlobalTransactionalDomainService;

    public void appTran(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTry(accountNumber,amount,customerId);
        demoGlobalTransactionalDomainService.sagaTx(accountNumber,amount,customerId);
    }

    @GlobalTransactional(name="DemoGlobalTransactionalApplicationService.appTranWithAnnotation")
    public void appTranWithAnnotation(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTryWithAnnotation(accountNumber,amount,customerId);
        demoGlobalTransactionalDomainService.sagaTxWithAnnotation(accountNumber,amount,customerId);
    }

    @GlobalTransactional(name="DemoGlobalTransactionalApplicationService.appTranWithAnnotationMandatory",propagation = Propagation.MANDATORY)
    public void appTranWithAnnotationMandatory(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTryWithAnnotation(accountNumber,amount,customerId);
        demoGlobalTransactionalDomainService.sagaTxWithAnnotation(accountNumber,amount,customerId);
    }

    @GlobalTransactional(name="DemoGlobalTransactionalApplicationService.appTranWithAnnotationNotSupported",propagation = Propagation.NOT_SUPPORTED)
    public void appTranWithAnnotationNotSupported(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTryWithAnnotation(accountNumber,amount,customerId);
        demoGlobalTransactionalDomainService.sagaTxWithAnnotation(accountNumber,amount,customerId);
    }

    @GlobalTransactional(name="DemoGlobalTransactionalApplicationService.appTranWithAnnotationNever",propagation = Propagation.NEVER)
    public void appTranWithAnnotationNever(String accountNumber, BigDecimal amount, long customerId) {
        demoGlobalTransactionalDomainService.tccTryWithAnnotation(accountNumber,amount,customerId);
        demoGlobalTransactionalDomainService.sagaTxWithAnnotation(accountNumber,amount,customerId);
    }
}
