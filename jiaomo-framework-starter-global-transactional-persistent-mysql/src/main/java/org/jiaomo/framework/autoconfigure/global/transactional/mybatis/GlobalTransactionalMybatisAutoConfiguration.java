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

package org.jiaomo.framework.autoconfigure.global.transactional.mybatis;


import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.mapper.TxGlobalMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.TxAutonomousService;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.TxContextService;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.TxGlobalService;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.impl.TxAutonomousServiceImpl;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.impl.TxContextServiceImpl;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.service.impl.TxGlobalServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 * {@code @date} 2023-08-26
 */

@Configuration
@MapperScan(basePackageClasses = {TxGlobalMapper.class})
public class GlobalTransactionalMybatisAutoConfiguration {
    @Bean
    @ConditionalOnClass(value = {TxAutonomousService.class})
    @ConditionalOnMissingBean(value = {TxAutonomousService.class})
    public TxAutonomousService txAutonomousService() {
        return new TxAutonomousServiceImpl();
    }

    @Bean
    @ConditionalOnClass(value = {TxContextService.class})
    @ConditionalOnMissingBean(value = {TxContextService.class})
    public TxContextService txContextService() {
        return new TxContextServiceImpl();
    }

    @Bean
    @ConditionalOnClass(value = {TxGlobalService.class})
    @ConditionalOnMissingBean(value = {TxGlobalService.class})
    public TxGlobalService txGlobalService() {
        return new TxGlobalServiceImpl();
    }
}
