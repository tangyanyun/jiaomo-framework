package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingEiFunction<T,T1,T2,T3,T4,R,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingEiFunction.class);

    R apply(T t,T1 t1,T2 t2,T3 t3,T4 t4) throws E;

    static <T,T1,T2,T3,T4,R,E extends Exception> EiFunction<T,T1,T2,T3,T4,R> toEiFunction(ThrowingEiFunction<? super T,? super T1,? super T2,? super T3,? super T4,? extends R,E> throwingEiFunction) {
        return toEiFunction(throwingEiFunction,true);
    }
    static <T,T1,T2,T3,T4,R,E extends Exception> EiFunction<T,T1,T2,T3,T4,R> toEiFunctionWithoutThrowing(ThrowingEiFunction<? super T,? super T1,? super T2,? super T3,? super T4,? extends R,E> throwingEiFunction) {
        return toEiFunction(throwingEiFunction,false);
    }

    static <T,T1,T2,T3,T4,R,E extends Exception> EiFunction<T,T1,T2,T3,T4,R> toEiFunction(ThrowingEiFunction<? super T,? super T1,? super T2,? super T3,? super T4,? extends R,E> throwingEiFunction, boolean isThrowing) {
        return (t,t1,t2,t3,t4) -> {
            try {
                return throwingEiFunction.apply(t,t1,t2,t3,t4);
            } catch (Exception e) {
                log.debug("toEiFunction isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return null;
                }
            }
        };
    }

    static <T,T1,T2,T3,T4,R,E extends Exception> R apply(ThrowingEiFunction<? super T,? super T1,? super T2,? super T3,? super T4,? extends R,E> throwingEiFunction, T t, T1 t1, T2 t2, T3 t3, T4 t4) {
        return toEiFunction(throwingEiFunction).apply(t,t1,t2,t3,t4);
    }
    static <T,T1,T2,T3,T4,R,E extends Exception> R applyWithoutThrowing(ThrowingEiFunction<? super T,? super T1,? super T2,? super T3,? super T4,? extends R,E> throwingEiFunction, T t, T1 t1, T2 t2, T3 t3, T4 t4) {
        return toEiFunctionWithoutThrowing(throwingEiFunction).apply(t,t1,t2,t3,t4);
    }
}
