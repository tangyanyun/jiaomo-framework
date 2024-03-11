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

package org.jiaomo.framework.commons.dp.observer;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public interface SubjectFunction<O extends ObserverFunction<O,S,T>,S extends SubjectFunction<O,S,T>,T> {
    //增加一个观察者
    boolean addObserver(ObserverFunction<O, S, T> o);

    //删除一个观察者
    boolean removeObserver(ObserverFunction<O, S, T> o);

    //通知所有观察者
    void notifyObservers(T t);
}
