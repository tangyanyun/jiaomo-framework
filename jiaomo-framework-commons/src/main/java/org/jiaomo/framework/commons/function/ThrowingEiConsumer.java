package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingEiConsumer<T,T1,T2,T3,T4,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingEiConsumer.class);

    void accept(T t,T1 t1,T2 t2,T3 t3,T4 t4) throws E;

    static <T,T1,T2,T3,T4,E extends Exception> EiConsumer<T,T1,T2,T3,T4> toEiConsumer(ThrowingEiConsumer<? super T,? super T1,? super T2,? super T3,? super T4,E> throwingEiConsumer) {
        return toEiConsumer(throwingEiConsumer,true);
    }
    static <T,T1,T2,T3,T4,E extends Exception> EiConsumer<T,T1,T2,T3,T4> toEiConsumerWithoutThrowing(ThrowingEiConsumer<? super T,? super T1,? super T2,? super T3,? super T4,E> throwingEiConsumer) {
        return toEiConsumer(throwingEiConsumer,false);
    }

    static <T,T1,T2,T3,T4,E extends Exception> EiConsumer<T,T1,T2,T3,T4> toEiConsumer(ThrowingEiConsumer<? super T,? super T1,? super T2,? super T3,? super T4,E> throwingEiConsumer, boolean isThrowing) {
        return (t,t1,t2,t3,t4) -> {
            try {
                throwingEiConsumer.accept(t,t1,t2,t3,t4);
            } catch (Exception e) {
                log.debug("toEiConsumer isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                }
            }
        };
    }

    static <T,T1,T2,T3,T4,E extends Exception> void accept(ThrowingEiConsumer<? super T,? super T1,? super T2,? super T3,? super T4,E> throwingEiConsumer, T t, T1 t1, T2 t2, T3 t3, T4 t4) {
        toEiConsumer(throwingEiConsumer).accept(t,t1,t2,t3,t4);
    }
    static <T,T1,T2,T3,T4,E extends Exception> void acceptWithoutThrowing(ThrowingEiConsumer<? super T,? super T1,? super T2,? super T3,? super T4,E> throwingEiConsumer, T t, T1 t1, T2 t2, T3 t3, T4 t4) {
        toEiConsumerWithoutThrowing(throwingEiConsumer).accept(t,t1,t2,t3,t4);
    }
}
