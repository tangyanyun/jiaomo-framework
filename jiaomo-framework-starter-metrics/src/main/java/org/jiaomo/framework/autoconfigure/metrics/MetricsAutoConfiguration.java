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

package org.jiaomo.framework.autoconfigure.metrics;


import org.jiaomo.framework.autoconfigure.metrics.adapter.MetricsAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 唐燕云 tangyanyun
 * @email tangyanyun@sina.com
 * @date 2023-08-26
 */

@Slf4j
@Configuration
public class MetricsAutoConfiguration {
    @Bean
    @ConditionalOnClass(value = {MetricsAdapter.class})
    @ConditionalOnMissingBean(value = {MetricsAdapter.class})
    public MetricsAdapter metricsAdapter() {
        return new MetricsAdapter();
    }
}
