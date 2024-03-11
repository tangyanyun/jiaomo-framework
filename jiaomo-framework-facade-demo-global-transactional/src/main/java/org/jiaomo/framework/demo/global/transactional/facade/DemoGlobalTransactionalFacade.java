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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;


@FeignClient(value = "jiaomo-framework-demo-global-transactional-server")
public interface DemoGlobalTransactionalFacade {
    @PostMapping("/demoGlobalTransactional/tccTry")
    ApiResult<Void> tccTry(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/tccTryDSWithAnnotation")
    ApiResult<Void> tccTryDSWithAnnotation(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/sagaTxDSWithAnnotation")
    ApiResult<Void> sagaTxDSWithAnnotation(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/appTranASWithAnnotation")
    ApiResult<Void> appTranASWithAnnotation(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/appTranMandatory")
    ApiResult<Void> appTranMandatory(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/appTranNotSupported")
    ApiResult<Void> appTranNotSupported(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/appTranNever")
    ApiResult<Void> appTranNever(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);

    @PostMapping("/demoGlobalTransactional/tccTryBenchmark")
    ApiResult<Void> tccTryBenchmark(@RequestParam("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount, @RequestParam(name = "customerId",required = false) long customerId);
}
