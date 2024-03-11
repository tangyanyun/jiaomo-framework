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

package org.jiaomo.framework.commons.function;

import java.util.Objects;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 *
 * four-Dimensional Consumer
 */

@FunctionalInterface
public interface DiConsumer<T,T1,T2,T3> {
    void accept(T t,T1 t1,T2 t2,T3 t3);

    default DiConsumer<T,T1,T2,T3> andThen(DiConsumer<? super T,? super T1,? super T2,? super T3> after) {
        Objects.requireNonNull(after);

        return (t,t1,t2,t3) -> {
            accept(t,t1,t2,t3);
            after.accept(t,t1,t2,t3);
        };
    }
}
