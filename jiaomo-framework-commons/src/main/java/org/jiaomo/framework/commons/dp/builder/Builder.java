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

package org.jiaomo.framework.commons.dp.builder;


import org.jiaomo.framework.commons.function.CiConsumer;
import org.jiaomo.framework.commons.function.DiConsumer;
import org.jiaomo.framework.commons.function.EiConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public class Builder<T> {
    private final Supplier<? extends T> instantiation;
    private final List<Consumer<? super T>> modifiers = new ArrayList<>();

    public Builder(Supplier<? extends T> instantiation) {
        this.instantiation = instantiation;
    }

    public static <T> Builder<T> of(Supplier<? extends T> instantiation) {
        return new Builder<>(instantiation);
    }

    public <T1> Builder<T> with(BiConsumer<? super T,? super T1> consumer,T1 t1) {
        modifiers.add(instance -> consumer.accept(instance,t1));
        return this;
    }

    public <T1,T2> Builder<T> with(CiConsumer<? super T,? super T1,? super T2> consumer, T1 t1, T2 t2) {
        modifiers.add(instance -> consumer.accept(instance,t1,t2));
        return this;
    }

    public <T1,T2,T3> Builder<T> with(DiConsumer<? super T,? super T1,? super T2,? super T3> consumer, T1 t1, T2 t2, T3 t3) {
        modifiers.add(instance -> consumer.accept(instance,t1,t2,t3));
        return this;
    }

    public <T1,T2,T3,T4> Builder<T> with(EiConsumer<? super T,? super T1,? super T2,? super T3,? super T4> consumer, T1 t1, T2 t2, T3 t3, T4 t4) {
        modifiers.add(instance -> consumer.accept(instance,t1,t2,t3,t4));
        return this;
    }

    public T build() {
        T instance = instantiation.get();
        modifiers.forEach(modifier -> modifier.accept(instance));
        modifiers.clear();
        return instance;
    }
}
