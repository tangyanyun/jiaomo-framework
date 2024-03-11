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

package org.jiaomo.framework.demo.global.transactional.facade;

import org.jiaomo.framework.commons.ApiResult;
import org.jiaomo.framework.global.transactional.annotation.AutonomousTransactional;
import org.jiaomo.framework.global.transactional.annotation.GlobalTransactional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;


@FeignClient(value = "jiaomo-framework-demo-global-transactional-server")
public interface DemoGlobalTransactionalWithAnnotationFacade {
    @AutonomousTransactional(name="DemoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation",commitMethod = "tccConfirm",rollbackMethod = "tccCancel")
    @PostMapping("/demoGlobalTransactional/tccTryWithAnnotation")
    ApiResult<Void> tccTryWithAnnotation(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/tccConfirm")
    ApiResult<Void> tccConfirm(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);
    @PostMapping("/demoGlobalTransactional/tccCancel")
    ApiResult<Void> tccCancel(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @AutonomousTransactional(name="DemoGlobalTransactionalWithAnnotationFacade.sagaTxWithAnnotation",rollbackMethod = "sagaRollback")
    @PostMapping("/demoGlobalTransactional/sagaTxWithAnnotation")
    ApiResult<Void> sagaTxWithAnnotation(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/sagaRollback")
    ApiResult<Void> sagaRollback(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @GlobalTransactional(name="DemoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotation")
    @PostMapping("/demoGlobalTransactional/appTranWithAnnotation")
    ApiResult<Void> appTranWithAnnotation(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @GlobalTransactional(name="DemoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotationRequiresNew",propagation = Propagation.REQUIRES_NEW)
    @PostMapping("/demoGlobalTransactional/appTranWithAnnotationRequiresNew")
    ApiResult<Void> appTranWithAnnotationRequiresNew(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);
    @AutonomousTransactional(name="DemoGlobalTransactionalWithAnnotationFacade.tccTryWithAnnotation0",commitMethod = "tccConfirm",rollbackMethod = "tccCancel")
    @PostMapping("/demoGlobalTransactional/tccTryWithAnnotation0")
    ApiResult<Void> tccTryWithAnnotation0(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @AutonomousTransactional(name="DemoGlobalTransactionalWithAnnotationFacade.sagaTxWithAnnotation0",rollbackMethod = "sagaRollback")
    @PostMapping("/demoGlobalTransactional/sagaTxWithAnnotation0")
    ApiResult<Void> sagaTxWithAnnotation0(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @GlobalTransactional(name="DemoGlobalTransactionalWithAnnotationFacade.appTranWithAnnotation0")
    @PostMapping("/demoGlobalTransactional/appTranWithAnnotation0")
    ApiResult<Void> appTranWithAnnotation0(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);
}
