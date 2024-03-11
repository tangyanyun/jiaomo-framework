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

package org.jiaomo.framework.autoconfigure.global.transactional.mybatis.po;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("tx_context")
public class TxContextPo implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String txCode;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String rootTxCode;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String parentTxCode;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private Byte txStatus;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String unitCode;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String globalName;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String autonomousName;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String argumentsSerial;
    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private String contextSerial;

    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private Date created;

    @TableField(insertStrategy = FieldStrategy.NOT_NULL,updateStrategy = FieldStrategy.NOT_NULL,whereStrategy = FieldStrategy.NOT_NULL)
    private Date updated;
}