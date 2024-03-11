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

package org.jiaomo.framework.commons.spring.web;

import org.jiaomo.framework.commons.function.ThrowingSupplier;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public class WebmvcUtils {

    public static void registerController(@NonNull BeanFactory beanFactory,Object handler,@NonNull Class<?> clazz)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Object handlerBean = handler instanceof String ? ThrowingSupplier.getWithoutThrowing(() -> beanFactory.getBean((String)handler)) : handler;
        if (handlerBean != null && handlerBean.getClass().getName().equals(clazz.getName())) {
            final RequestMappingHandlerMapping requestMappingHandlerMapping = beanFactory.getBean(RequestMappingHandlerMapping.class);
            Method method = requestMappingHandlerMapping.getClass().getSuperclass().getSuperclass().getDeclaredMethod("detectHandlerMethods",Object.class);
//          Method method = requestMappingHandlerMapping.getClass().getSuperclass().getSuperclass().getDeclaredMethod("processCandidateBean",String.class);
            method.setAccessible(true);
            method.invoke(requestMappingHandlerMapping,handlerBean);
        }
    }
}
