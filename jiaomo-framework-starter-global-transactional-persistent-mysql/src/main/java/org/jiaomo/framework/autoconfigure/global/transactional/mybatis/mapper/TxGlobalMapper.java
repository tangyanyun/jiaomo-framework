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
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxGlobalPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface TxGlobalMapper extends BaseMapper<TxGlobalPo> {

    @Insert({"<script>",
            "insert into tx_global (name,timeout_mills,propagation,rollback_for_name,application_name) ",
                "values (#{name},#{timeoutMills},#{propagation},#{rollbackForName},#{applicationName}) ON DUPLICATE KEY UPDATE ",
                "name = values(name),timeout_mills = values(timeout_mills),propagation = values(propagation),rollback_for_name = values(rollback_for_name),application_name = values(application_name)",
            "</script>"})
    int insertOrUpdate(TxGlobalPo txGlobalPo);

}