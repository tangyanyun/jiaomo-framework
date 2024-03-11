package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingPredicate<T,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingPredicate.class);

    boolean test(T t) throws E;

    static <T,E extends Exception> Predicate<T> toPredicate(ThrowingPredicate<? super T,E> throwingPredicate) {
        return toPredicate(throwingPredicate,true,false);
    }
    static <T,E extends Exception> Predicate<T> toPredicateWithoutThrowing(ThrowingPredicate<? super T,E> throwingPredicate,boolean withoutThrowingDefaultReturnValue) {
        return toPredicate(throwingPredicate,false,withoutThrowingDefaultReturnValue);
    }

    static <T,E extends Exception> Predicate<T> toPredicate(ThrowingPredicate<? super T,E> throwingPredicate,boolean isThrowing,boolean withoutThrowingDefaultReturnValue) {
        return t -> {
            try {
                return throwingPredicate.test(t);
            } catch (Exception e) {
                log.debug("toPredicate isThrowing:{} withoutThrowingDefaultReturnValue:{} {} {}",isThrowing,withoutThrowingDefaultReturnValue,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return withoutThrowingDefaultReturnValue;
                }
            }
        };
    }

    static <T,E extends Exception> boolean test(ThrowingPredicate<? super T,E> throwingPredicate,T t) {
        return toPredicate(throwingPredicate).test(t);
    }
    static <T,E extends Exception> boolean testWithoutThrowing(ThrowingPredicate<? super T,E> throwingPredicate,T t,boolean withoutThrowingDefaultReturnValue) {
        return toPredicateWithoutThrowing(throwingPredicate,withoutThrowingDefaultReturnValue).test(t);
    }
}
