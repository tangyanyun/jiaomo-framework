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

package org.jiaomo.framework.commons.po;

import java.util.List;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public abstract class PersistentObjectBaseComponent<T extends PersistentObject,M extends PersistentObjectBaseMapper<T,M>> {

    private Class<T> persistentObjectClass;
    private M mapper;

    protected PersistentObjectBaseComponent(Class<T> persistentObjectClass,M mapper) {
        this.persistentObjectClass = persistentObjectClass;
        this.mapper = mapper;
    }

    protected Class<T> getPersistentObjectClass() {
        return this.persistentObjectClass;
    }
    protected M getMapper() {
        return this.mapper;
    }

    public int insert(T record) {
        return getMapper().insert(record);
    }

    public int updateByPrimaryKey(T record) {
        return getMapper().updateByPrimaryKey(record);
    }

    public List<T> selectByParameters(T record) {
        return getMapper().selectByParameters(record);
    }

    public int selectCountByParameters(T record) {
        return getMapper().selectCountByParameters(record);
    }
}
