package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingCiConsumer<T,T1,T2,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingCiConsumer.class);

    void accept(T t,T1 t1,T2 t2) throws E;

    static <T,T1,T2,E extends Exception> CiConsumer<T,T1,T2> toCiConsumer(ThrowingCiConsumer<? super T,? super T1,? super T2,E> throwingCiConsumer) {
        return toCiConsumer(throwingCiConsumer,true);
    }
    static <T,T1,T2,E extends Exception> CiConsumer<T,T1,T2> toCiConsumerWithoutThrowing(ThrowingCiConsumer<? super T,? super T1,? super T2,E> throwingCiConsumer) {
        return toCiConsumer(throwingCiConsumer,false);
    }

    static <T,T1,T2,E extends Exception> CiConsumer<T,T1,T2> toCiConsumer(ThrowingCiConsumer<? super T,? super T1,? super T2,E> throwingCiConsumer, boolean isThrowing) {
        return (t,t1,t2) -> {
            try {
                throwingCiConsumer.accept(t,t1,t2);
            } catch (Exception e) {
                log.debug("toCiConsumer isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                }
            }
        };
    }

    static <T,T1,T2,E extends Exception> void accept(ThrowingCiConsumer<? super T,? super T1,? super T2,E> throwingCiConsumer,T t,T1 t1,T2 t2) {
        toCiConsumer(throwingCiConsumer).accept(t,t1,t2);
    }
    static <T,T1,T2,E extends Exception> void acceptWithoutThrowing(ThrowingCiConsumer<? super T,? super T1,? super T2,E> throwingCiConsumer,T t,T1 t1,T2 t2) {
        toCiConsumerWithoutThrowing(throwingCiConsumer).accept(t,t1,t2);
    }
}
