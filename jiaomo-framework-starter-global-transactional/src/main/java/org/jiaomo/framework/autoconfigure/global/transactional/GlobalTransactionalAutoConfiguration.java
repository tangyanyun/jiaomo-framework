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

package org.jiaomo.framework.autoconfigure.global.transactional;


import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;
import org.jiaomo.framework.autoconfigure.global.transactional.listener.AutonomousTransactionalListener;
import org.jiaomo.framework.autoconfigure.global.transactional.listener.GlobalTransactionalListener;
import org.jiaomo.framework.autoconfigure.global.transactional.listener.impl.SimpleAutonomousTransactionalListenerImpl;
import org.jiaomo.framework.autoconfigure.global.transactional.listener.impl.SimpleGlobalTransactionalListenerImpl;
import org.jiaomo.framework.autoconfigure.global.transactional.visitor.TxContextVisitor;
import org.jiaomo.framework.autoconfigure.global.transactional.interceptor.*;
import org.jiaomo.framework.autoconfigure.global.transactional.dao.TxContextDao;
import org.jiaomo.framework.commons.spring.web.WebmvcUtils;
import org.jiaomo.framework.commons.function.ThrowingRunnable;
import org.jiaomo.framework.global.transactional.annotation.AutonomousTransactional;
import org.jiaomo.framework.global.transactional.annotation.GlobalTransactional;
import org.jiaomo.framework.commons.UUIDCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Slf4j
@Configuration
public class GlobalTransactionalAutoConfiguration {
    @Value("${global-transactional.thread-pool.core-pool-size:4}")
    private int corePoolSize;
    @Value("${global-transactional.thread-pool.maximum-pool-size:128}")
    private int maximumPoolSize;
    @Value("${global-transactional.thread-pool.work-queue-capacity:4}")
    private int workQueueCapacity;

    @Bean
    @ConditionalOnMissingBean(name = {"globalTransactionalThreadPoolExecutor"})
    public ThreadPoolExecutor globalTransactionalThreadPoolExecutor() {
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 30, TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(workQueueCapacity),
                runnable -> new Thread(runnable,"globalTransactionalThreadPool-" + runnable.hashCode()),
                (runnable, executor) -> {
                    try {
                        executor.getQueue().put(runnable);
                    } catch (InterruptedException e) {
                        log.error("globalTransactionalThreadPoolExecutor Queue put InterruptedException",e);
                    }
                });
    }

    @Bean
    @ConditionalOnClass(value = {TxContextDao.class})
    @ConditionalOnMissingBean(value = {TxContextDao.class})
    public TxContextDao txContextDao() {
        return new TxContextDao();
    }

    @Bean
    @ConditionalOnClass(value = {UUIDCodeGenerator.class})
    @ConditionalOnMissingBean(name = {"uuidCodeGeneratorTxCode"})
    public UUIDCodeGenerator uuidCodeGeneratorTxCode() {
        return new UUIDCodeGenerator("X_");
    }

    @Bean
    @ConditionalOnClass(value = {GlobalTransactionalRequestInterceptor.class})
    @ConditionalOnMissingBean(value = {GlobalTransactionalRequestInterceptor.class})
    public GlobalTransactionalRequestInterceptor globalTransactionalRequestInterceptor() {
        return new GlobalTransactionalRequestInterceptor();
    }

    @Bean
    @ConditionalOnClass(value = {GlobalTransactionalClientHttpRequestInterceptor.class})
    @ConditionalOnMissingBean(value = {GlobalTransactionalClientHttpRequestInterceptor.class})
    public GlobalTransactionalClientHttpRequestInterceptor globalTransactionalClientHttpRequestInterceptor() {
        return new GlobalTransactionalClientHttpRequestInterceptor();
    }

    @LoadBalanced
    @Bean
    @ConditionalOnMissingBean(name = {"globalTransactionalRestTemplate"})
    public RestTemplate globalTransactionalRestTemplate(@Autowired(required = false) OkHttpClient okHttpClient) {
        ClientHttpRequestFactory clientHttpRequestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient != null ? okHttpClient :
                new OkHttpClient().newBuilder()
                        .connectionPool(new ConnectionPool(200,60L,TimeUnit.SECONDS))
                        .connectTimeout(5,TimeUnit.SECONDS)
                        .readTimeout(60,TimeUnit.SECONDS)
                        .writeTimeout(5,TimeUnit.SECONDS)
                        .hostnameVerifier((hostname, session) -> true)
                        .build());
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.getInterceptors().add(globalTransactionalClientHttpRequestInterceptor());
        return restTemplate;
    }

    @Bean
    @ConditionalOnClass(value = {GlobalTransactionalListener.class})
    @ConditionalOnMissingBean(value = {GlobalTransactionalListener.class})
    public GlobalTransactionalListener globalTransactionalListener() {
        return new SimpleGlobalTransactionalListenerImpl();
    }

    @Bean
    @ConditionalOnClass(value = {AutonomousTransactionalListener.class})
    @ConditionalOnMissingBean(value = {AutonomousTransactionalListener.class})
    public AutonomousTransactionalListener autonomousTransactionalListener() {
        return new SimpleAutonomousTransactionalListenerImpl();
    }

    @Bean
    @ConditionalOnClass(value = {GlobalTransactionalService.class})
    @ConditionalOnMissingBean(value = {GlobalTransactionalService.class})
    public GlobalTransactionalService globalTransactionalService() {
        return new GlobalTransactionalService();
    }

    @Bean
    @ConditionalOnClass(value = {TxContextVisitor.class})
    @ConditionalOnMissingBean(value = {TxContextVisitor.class})
    public TxContextVisitor<TxContextVisitorImpl,TxContext,TxContext.AutonomousContext> txContextVisitor() {
        return new TxContextVisitorImpl(globalTransactionalService());
    }

    @Bean
    @ConditionalOnClass(value = {GlobalTransactionalInterceptor.class})
    @ConditionalOnMissingBean(value = {GlobalTransactionalInterceptor.class})
    public GlobalTransactionalInterceptor globalTransactionalInterceptor() {
        return new GlobalTransactionalInterceptor();
    }

    @Bean
    @ConditionalOnClass(value = {AutonomousTransactionalInterceptor.class})
    @ConditionalOnMissingBean(value = {AutonomousTransactionalInterceptor.class})
    public AutonomousTransactionalInterceptor autonomousTransactionalInterceptor() {
        return new AutonomousTransactionalInterceptor();
    }

    @Bean
    public Advisor globalTransactionalAdvisor() {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, GlobalTransactional.class);
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setPointcut(pointcut);
        advisor.setAdvice(globalTransactionalInterceptor());
        advisor.setOrder(Integer.MAX_VALUE);
        log.debug("globalTransactionalAdvisor:{}",advisor);
        return advisor;
    }

    @Bean
    public Advisor autonomousTransactionalAdvisor() {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, AutonomousTransactional.class);
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setPointcut(pointcut);
        advisor.setAdvice(autonomousTransactionalInterceptor());
        advisor.setOrder(Integer.MAX_VALUE);
        log.debug("autonomousTransactionalAdvisor:{}",advisor);
        return advisor;
    }

    @Bean
    @ConditionalOnClass(value = {GlobalTransactionalController.class})
    @ConditionalOnMissingBean(value = {GlobalTransactionalController.class})
    public GlobalTransactionalController globalTransactionalController(@Autowired ApplicationContext applicationContext) {
        GlobalTransactionalController controller = new GlobalTransactionalController(txContextVisitor(),globalTransactionalService());
        ThrowingRunnable.run(() -> WebmvcUtils.registerController(applicationContext,
                controller,GlobalTransactionalController.class));
        return controller;
    }

    @Bean
    @ConditionalOnClass(value = {GlobalTransactionalWebMvcHandlerInterceptor.class})
    @ConditionalOnMissingBean(value = {GlobalTransactionalWebMvcHandlerInterceptor.class})
    public GlobalTransactionalWebMvcHandlerInterceptor globalTransactionalWebMvcHandlerInterceptor() {
        return new GlobalTransactionalWebMvcHandlerInterceptor();
    }

    @Bean
    public WebMvcConfigurer globalTransactionalWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(globalTransactionalWebMvcHandlerInterceptor())
                        .addPathPatterns("/**");
            }
        };
    }
}
