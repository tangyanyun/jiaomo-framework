package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingFunction<T,R,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingFunction.class);

    R apply(T t) throws E;

    static <T,R,E extends Exception> Function<T,R> toFunction(ThrowingFunction<? super T,? extends R,E> throwingFunction) {
        return toFunction(throwingFunction,true);
    }
    static <T,R,E extends Exception> Function<T,R> toFunctionWithoutThrowing(ThrowingFunction<? super T,? extends R,E> throwingFunction) {
        return toFunction(throwingFunction,false);
    }

    static <T,R,E extends Exception> Function<T,R> toFunction(ThrowingFunction<? super T,? extends R,E> throwingFunction,boolean isThrowing) {
        return t -> {
            try {
                return throwingFunction.apply(t);
            } catch (Exception e) {
                log.debug("toFunction isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return null;
                }
            }
        };
    }

    static <T,R,E extends Exception> R apply(ThrowingFunction<? super T,? extends R,E> throwingFunction,T t) {
        return toFunction(throwingFunction).apply(t);
    }
    static <T,R,E extends Exception> R applyWithoutThrowing(ThrowingFunction<? super T,? extends R,E> throwingFunction,T t) {
        return toFunctionWithoutThrowing(throwingFunction).apply(t);
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
        throw (T)t;
    }

    @SuppressWarnings("unchecked")
    static <T> T sneakyClassCast(Object t) {
        return (T)t;
    }
}
