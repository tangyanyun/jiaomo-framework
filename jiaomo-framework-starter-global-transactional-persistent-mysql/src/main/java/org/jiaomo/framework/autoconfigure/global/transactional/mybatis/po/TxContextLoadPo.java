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


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TxContextLoadPo implements Serializable {
    private String txCode;
    private String rootTxCode;
    private String parentTxCode;
    private Byte txStatus;
    private String unitCode;
    private String globalName;
    private String autonomousName;
    private String argumentsSerial;
    private String contextSerial;
    private Date created;

    private Integer timeoutMills;
    private Integer propagation;
    private String rollbackForName;
    private String globalApplicationName;

    private String commitMethod;
    private String rollbackMethod;
    private Byte commitAsync;
    private Byte rollbackAsync;
    private String targetClassName;
    private String parameterTypesName;
    private String autonomousApplicationName;
    private String beanName;
}