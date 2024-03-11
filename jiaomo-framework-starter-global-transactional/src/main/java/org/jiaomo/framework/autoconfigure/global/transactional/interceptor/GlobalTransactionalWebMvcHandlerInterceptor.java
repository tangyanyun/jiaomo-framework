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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
public class GlobalTransactionalWebMvcHandlerInterceptor implements HandlerInterceptor {
    static final String HEADER_NAME_GLOBAL_TRANSACTIONAL_TX_CODE = "GLOBAL-TRANSACTIONAL-TX-CODE-MEE1g5CpIV1r9HVdhPh";
    static final String SEPARATOR = "|";

    @Override
    public boolean preHandle(HttpServletRequest request,@Nonnull HttpServletResponse response,@Nonnull Object handler) throws Exception {
        String txCode = request.getHeader(HEADER_NAME_GLOBAL_TRANSACTIONAL_TX_CODE);
        if (StringUtils.isNotBlank(txCode)) {
            TxContextHolder.setCurrentTxCode(txCode);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,@Nonnull HttpServletResponse response,
                                @Nonnull Object handler,@Nullable Exception ex) throws Exception {
        if (StringUtils.isNotBlank(request.getHeader(HEADER_NAME_GLOBAL_TRANSACTIONAL_TX_CODE))) {
            TxContextHolder.removeCurrentTxCode();
            TxContextHolder.removeCurrent();
        }
    }
}