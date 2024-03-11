package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingBiFunction<T,U,R,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingBiFunction.class);

    R apply(T t,U u) throws E;

    static <T,U,R,E extends Exception> BiFunction<T,U,R> toBiFunction(ThrowingBiFunction<? super T,? super U,? extends R,E> throwingBiFunction) {
        return toBiFunction(throwingBiFunction,true);
    }
    static <T,U,R,E extends Exception> BiFunction<T,U,R> toBiFunctionWithoutThrowing(ThrowingBiFunction<? super T,? super U,? extends R,E> throwingBiFunction) {
        return toBiFunction(throwingBiFunction,false);
    }

    static <T,U,R,E extends Exception> BiFunction<T,U,R> toBiFunction(ThrowingBiFunction<? super T,? super U,? extends R,E> throwingBiFunction,boolean isThrowing) {
        return (t,u) -> {
            try {
                return throwingBiFunction.apply(t,u);
            } catch (Exception e) {
                log.debug("toBiFunction isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return null;
                }
            }
        };
    }

    static <T,U,R,E extends Exception> R apply(ThrowingBiFunction<? super T,? super U,? extends R,E> throwingBiFunction,T t,U u) {
        return toBiFunction(throwingBiFunction).apply(t,u);
    }
    static <T,U,R,E extends Exception> R applyWithoutThrowing(ThrowingBiFunction<? super T,? super U,? extends R,E> throwingBiFunction,T t,U u) {
        return toBiFunctionWithoutThrowing(throwingBiFunction).apply(t,u);
    }
}
