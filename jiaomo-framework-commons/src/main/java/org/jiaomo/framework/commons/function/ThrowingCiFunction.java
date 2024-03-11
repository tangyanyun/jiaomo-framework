package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingCiFunction<T,T1,T2,R,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingCiFunction.class);

    R apply(T t,T1 t1,T2 t2) throws E;

    static <T,T1,T2,R,E extends Exception> CiFunction<T,T1,T2,R> toCiFunction(ThrowingCiFunction<? super T,? super T1,? super T2,? extends R,E> throwingCiFunction) {
        return toCiFunction(throwingCiFunction,true);
    }
    static <T,T1,T2,R,E extends Exception> CiFunction<T,T1,T2,R> toCiFunctionWithoutThrowing(ThrowingCiFunction<? super T,? super T1,? super T2,? extends R,E> throwingCiFunction) {
        return toCiFunction(throwingCiFunction,false);
    }

    static <T,T1,T2,R,E extends Exception> CiFunction<T,T1,T2,R> toCiFunction(ThrowingCiFunction<? super T,? super T1,? super T2,? extends R,E> throwingCiFunction,boolean isThrowing) {
        return (t,t1,t2) -> {
            try {
                return throwingCiFunction.apply(t,t1,t2);
            } catch (Exception e) {
                log.debug("toCiFunction isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return null;
                }
            }
        };
    }

    static <T,T1,T2,R,E extends Exception> R apply(ThrowingCiFunction<? super T,? super T1,? super T2,? extends R,E> throwingCiFunction,T t,T1 t1,T2 t2) {
        return toCiFunction(throwingCiFunction).apply(t,t1,t2);
    }
    static <T,T1,T2,R,E extends Exception> R applyWithoutThrowing(ThrowingCiFunction<? super T,? super T1,? super T2,? extends R,E> throwingCiFunction,T t,T1 t1,T2 t2) {
        return toCiFunctionWithoutThrowing(throwingCiFunction).apply(t,t1,t2);
    }
}
