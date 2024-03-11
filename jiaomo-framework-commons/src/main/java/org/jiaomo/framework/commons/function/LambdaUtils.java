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

import java.util.function.*;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public interface LambdaUtils {

    static void run(Runnable runnable) {
        runnable.run();
    }

    static <T> T get(Supplier<? extends T> supplier) {
        return supplier.get();
    }

    static <T> boolean test(Predicate<? super T> predicate,T t) {
        return predicate.test(t);
    }
    static <T,U> boolean test(BiPredicate<? super T,? super U> predicate,T t,U u) {
        return predicate.test(t,u);
    }

    static <T> void accept(Consumer<? super T> consumer,T t) {
        consumer.accept(t);
    }
    static <T,U> void accept(BiConsumer<? super T,? super U> consumer,T t,U u) {
        consumer.accept(t,u);
    }
    static <T,T1,T2> void accept(CiConsumer<? super T,? super T1,? super T2> consumer,T t,T1 t1,T2 t2) {
        consumer.accept(t,t1,t2);
    }
    static <T,T1,T2,T3> void accept(DiConsumer<? super T,? super T1,? super T2,? super T3> consumer,T t,T1 t1,T2 t2,T3 t3) {
        consumer.accept(t,t1,t2,t3);
    }
    static <T,T1,T2,T3,T4> void accept(EiConsumer<? super T,? super T1,? super T2,? super T3,? super T4> consumer,T t,T1 t1,T2 t2,T3 t3,T4 t4) {
        consumer.accept(t,t1,t2,t3,t4);
    }

    static <T,R> R apply(Function<? super T, ? extends R> function, T t) {
        return function.apply(t);
    }
    static <T,U,R> R apply(BiFunction<? super T,? super U,? extends R> function,T t,U u) {
        return function.apply(t,u);
    }
    static <T,T1,T2,R> R apply(CiFunction<? super T,? super T1,? super T2,? extends R> function,T t,T1 t1,T2 t2) {
        return function.apply(t,t1,t2);
    }
    static <T,T1,T2,T3,R> R apply(DiFunction<? super T,? super T1,? super T2,? super T3,? extends R> function,T t,T1 t1,T2 t2,T3 t3) {
        return function.apply(t,t1,t2,t3);
    }
    static <T,T1,T2,T3,T4,R> R apply(EiFunction<? super T,? super T1,? super T2,? super T3,? super T4,? extends R> function,T t,T1 t1,T2 t2,T3 t3,T4 t4) {
        return function.apply(t,t1,t2,t3,t4);
    }
}
