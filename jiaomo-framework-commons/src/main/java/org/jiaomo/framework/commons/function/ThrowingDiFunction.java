package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingDiFunction<T,T1,T2,T3,R,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingDiFunction.class);

    R apply(T t,T1 t1,T2 t2,T3 t3) throws E;

    static <T,T1,T2,T3,R,E extends Exception> DiFunction<T,T1,T2,T3,R> toDiFunction(ThrowingDiFunction<? super T,? super T1,? super T2,? super T3,? extends R,E> throwingDiFunction) {
        return toDiFunction(throwingDiFunction,true);
    }
    static <T,T1,T2,T3,R,E extends Exception> DiFunction<T,T1,T2,T3,R> toDiFunctionWithoutThrowing(ThrowingDiFunction<? super T,? super T1,? super T2,? super T3,? extends R,E> throwingDiFunction) {
        return toDiFunction(throwingDiFunction,false);
    }

    static <T,T1,T2,T3,R,E extends Exception> DiFunction<T,T1,T2,T3,R> toDiFunction(ThrowingDiFunction<? super T,? super T1,? super T2,? super T3,? extends R,E> throwingDiFunction, boolean isThrowing) {
        return (t,t1,t2,t3) -> {
            try {
                return throwingDiFunction.apply(t,t1,t2,t3);
            } catch (Exception e) {
                log.debug("toDiFunction isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return null;
                }
            }
        };
    }

    static <T,T1,T2,T3,R,E extends Exception> R apply(ThrowingDiFunction<? super T,? super T1,? super T2,? super T3,? extends R,E> throwingDiFunction,T t,T1 t1,T2 t2,T3 t3) {
        return toDiFunction(throwingDiFunction).apply(t,t1,t2,t3);
    }
    static <T,T1,T2,T3,R,E extends Exception> R applyWithoutThrowing(ThrowingDiFunction<? super T,? super T1,? super T2,? super T3,? extends R,E> throwingDiFunction,T t,T1 t1,T2 t2,T3 t3) {
        return toDiFunctionWithoutThrowing(throwingDiFunction).apply(t,t1,t2,t3);
    }
}
