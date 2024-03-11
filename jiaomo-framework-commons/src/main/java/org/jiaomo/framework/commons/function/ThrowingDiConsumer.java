package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingDiConsumer<T,T1,T2,T3,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingDiConsumer.class);

    void accept(T t,T1 t1,T2 t2,T3 t3) throws E;

    static <T,T1,T2,T3,E extends Exception> DiConsumer<T,T1,T2,T3> toDiConsumer(ThrowingDiConsumer<? super T,? super T1,? super T2,? super T3,E> throwingDiConsumer) {
        return toDiConsumer(throwingDiConsumer,true);
    }
    static <T,T1,T2,T3,E extends Exception> DiConsumer<T,T1,T2,T3> toDiConsumerWithoutThrowing(ThrowingDiConsumer<? super T,? super T1,? super T2,? super T3,E> throwingDiConsumer) {
        return toDiConsumer(throwingDiConsumer,false);
    }

    static <T,T1,T2,T3,E extends Exception> DiConsumer<T,T1,T2,T3> toDiConsumer(ThrowingDiConsumer<? super T,? super T1,? super T2,? super T3,E> throwingDiConsumer, boolean isThrowing) {
        return (t,t1,t2,t3) -> {
            try {
                throwingDiConsumer.accept(t,t1,t2,t3);
            } catch (Exception e) {
                log.debug("toDiConsumer isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                }
            }
        };
    }

    static <T,T1,T2,T3,E extends Exception> void accept(ThrowingDiConsumer<? super T,? super T1,? super T2,? super T3,E> throwingDiConsumer,T t,T1 t1,T2 t2,T3 t3) {
        toDiConsumer(throwingDiConsumer).accept(t,t1,t2,t3);
    }
    static <T,T1,T2,T3,E extends Exception> void acceptWithoutThrowing(ThrowingDiConsumer<? super T,? super T1,? super T2,? super T3,E> throwingDiConsumer,T t,T1 t1,T2 t2,T3 t3) {
        toDiConsumerWithoutThrowing(throwingDiConsumer).accept(t,t1,t2,t3);
    }
}
