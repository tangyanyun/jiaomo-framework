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

package org.jiaomo.framework.autoconfigure.global.transactional.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxAutonomousPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TxAutonomousMapper extends BaseMapper<TxAutonomousPo> {

    @Insert({"<script>",
            "insert into tx_autonomous (name,",
                "<if test='commitMethod != null'>",
                    "commit_method,",
                "</if><if test='rollbackMethod != null'>",
                    "rollback_method,",
                "</if>",
                "commit_async,rollback_async,",
                "<if test='applicationName != null'>",
                    "application_name,",
                "</if><if test='beanName != null'>",
                    "bean_name,",
                "</if><if test='targetClassName != null'>",
                    "target_class_name,",
                "</if>",
                "parameter_types_name) ",
            "values (#{name},",
                "<if test='commitMethod != null'>",
                    "#{commitMethod},",
                "</if><if test='rollbackMethod != null'>",
                    "#{rollbackMethod},",
                "</if>",
                "#{commitAsync},#{rollbackAsync},",
                "<if test='applicationName != null'>",
                    "#{applicationName},",
                "</if><if test='beanName != null'>",
                    "#{beanName},",
                "</if><if test='targetClassName != null'>",
                    "#{targetClassName},",
                "</if>",
                "#{parameterTypesName}) ",
            "ON DUPLICATE KEY UPDATE name = values(name),",
                "<if test='commitMethod != null'>",
                    "commit_method = values(commit_method),",
                "</if><if test='rollbackMethod != null'>",
                    "rollback_method = values(rollback_method),",
                "</if>",
                "commit_async = values(commit_async),rollback_async = values(rollback_async),",
                "<if test='applicationName != null'>",
                    "application_name = values(application_name),",
                "</if><if test='beanName != null'>",
                    "bean_name = values(bean_name),",
                "</if><if test='targetClassName != null'>",
                    "target_class_name = values(target_class_name),",
                "</if>",
                "parameter_types_name = values(parameter_types_name)",
            "</script>"})
    int insertOrUpdate(TxAutonomousPo txAutonomousPo);

}