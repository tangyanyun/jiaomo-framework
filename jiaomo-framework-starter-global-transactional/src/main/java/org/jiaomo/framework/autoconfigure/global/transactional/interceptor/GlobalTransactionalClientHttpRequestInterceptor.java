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

import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxBase;
import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
public class GlobalTransactionalClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    @Override
    @Nonnull
    public ClientHttpResponse intercept(HttpRequest request,@Nonnull byte[] body,@Nonnull ClientHttpRequestExecution execution) throws IOException {
        if (request.getURI().getPath().contains(GlobalTransactionalController.PATH_PREFIX))
            return execution.execute(request, body);

        String txCode = Optional.ofNullable(TxContextHolder.getCurrent())
                .filter(txBase -> txBase instanceof TxContext).map(TxBase::getTxCode).orElse(null);
        if (StringUtils.isNotBlank(txCode)) {
            TxContext txContext = (TxContext)TxContextHolder.getCurrent();
            request.getHeaders().add(GlobalTransactionalWebMvcHandlerInterceptor.HEADER_NAME_GLOBAL_TRANSACTIONAL_TX_CODE,
                    txContext.getGlobal().getApplicationName() +
                            GlobalTransactionalWebMvcHandlerInterceptor.SEPARATOR +
                            txContext.getUnitCode() +
                            GlobalTransactionalWebMvcHandlerInterceptor.SEPARATOR + txCode);
            log.debug("ClientRequestInterceptor {} applicationName:{} unitCode:{} txCode:{}",
                    GlobalTransactionalWebMvcHandlerInterceptor.HEADER_NAME_GLOBAL_TRANSACTIONAL_TX_CODE,
                    txContext.getGlobal().getApplicationName(),txContext.getUnitCode(),txCode);
        }

        return execution.execute(request, body);
    }
}
