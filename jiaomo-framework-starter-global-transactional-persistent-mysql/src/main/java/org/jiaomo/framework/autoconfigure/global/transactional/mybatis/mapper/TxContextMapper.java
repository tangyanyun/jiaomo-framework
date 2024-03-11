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
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxContextLoadPo;
import org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po.TxContextPo;
import org.apache.ibatis.annotations.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;


@Mapper
public interface TxContextMapper extends BaseMapper<TxContextPo> {

    @Select("select created from tx_context where tx_code = #{txCode}")
    @Nullable
    Date selectCreatedByTxCode(@Param("txCode") String txCode);

    @Select("select now()")
    Date selectNow();

    @ResultType(TxContextLoadPo.class)
    @Select({"<script>",
            "select c.tx_code tx_code,c.root_tx_code root_tx_code,c.parent_tx_code parent_tx_code,c.tx_status tx_status,c.unit_code unit_code,c.global_name global_name,c.autonomous_name autonomous_name,c.arguments_serial arguments_serial,c.context_serial context_serial,c.created created,",
                    "g.timeout_mills timeout_mills,g.propagation propagation,g.rollback_for_name rollback_for_name,g.application_name global_application_name,",
                    "a.commit_method commit_method,a.rollback_method rollback_method,a.commit_async commit_async,a.rollback_async rollback_async,a.application_name autonomous_application_name,a.target_class_name target_class_name,a.parameter_types_name parameter_types_name ",
            "from tx_context c left join tx_global g on c.global_name = g.name left join tx_autonomous a on c.autonomous_name = a.name ",
            "<where>",
            "<if test='txCode != null'>",
                "and c.tx_code = #{txCode}",
            "</if><if test='rootTxCode != null'>",
                "and c.root_tx_code = #{rootTxCode}",
            "</if><if test='parentTxCode != null'>",
                "and c.parent_tx_code = #{parentTxCode}",
            "</if>",
            "</where>",
            "</script>"})
    @Nonnull
    List<TxContextLoadPo> load(@Param("txCode") String txCode, @Param("rootTxCode") String rootTxCode, @Param("parentTxCode") String parentTxCode);
//  List<Map<String,Object>> load(@Param("txCode") String txCode,@Param("rootTxCode") String rootTxCode,@Param("parentTxCode") String parentTxCode);

    @ResultType(TxContextLoadPo.class)
    @Select({"<script>",
            "select c.tx_code tx_code,c.root_tx_code root_tx_code,c.parent_tx_code parent_tx_code,c.tx_status tx_status,c.unit_code unit_code,c.global_name global_name,c.autonomous_name autonomous_name,c.arguments_serial arguments_serial,c.context_serial context_serial,c.created created,",
            "g.timeout_mills timeout_mills,g.propagation propagation,g.rollback_for_name rollback_for_name,g.application_name global_application_name ",
            "from tx_context c join tx_global g on c.global_name = g.name ",
            "<where>",
            "<if test='globalApplicationName != null'>",
                "and g.application_name = #{globalApplicationName}",
            "</if><if test='unitCode != null'>",
                "and c.unit_code = #{unitCode}",
            "</if><if test='statuses != null'>",
                "and c.tx_status in ",
                "<foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
                    "#{status}",
                "</foreach>",
            "</if><if test='recoverTime != null'>",
                "and c.created &lt; date_sub(#{recoverTime},INTERVAL (g.timeout_mills / 1000) SECOND )",
            "</if>",
            "</where>",
            "</script>"})
    @Nonnull
    List<TxContextLoadPo> findByGlobalApplicationNameAndUnitCodeAndStatusesAndRecoverTime(@Param("globalApplicationName") String globalApplicationName,
                                                                                          @Param("unitCode") String unitCode,
                                                                                          @Param("statuses") Collection<Byte> statuses,
                                                                                          @Param("recoverTime") Date recoverTime);
}