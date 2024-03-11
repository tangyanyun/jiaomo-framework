package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiPredicate;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingBiPredicate<T,U,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingBiPredicate.class);

    boolean test(T t,U u) throws E;

    static <T,U,E extends Exception> BiPredicate<T,U> toBiPredicate(ThrowingBiPredicate<? super T,? super U,E> throwingBiPredicate) {
        return toBiPredicate(throwingBiPredicate,true,false);
    }
    static <T,U,E extends Exception> BiPredicate<T,U> toBiPredicateWithoutThrowing(ThrowingBiPredicate<? super T,? super U,E> throwingBiPredicate,boolean withoutThrowingDefaultReturnValue) {
        return toBiPredicate(throwingBiPredicate,false,withoutThrowingDefaultReturnValue);
    }

    static <T,U,E extends Exception> BiPredicate<T,U> toBiPredicate(ThrowingBiPredicate<? super T,? super U,E> throwingBiPredicate,boolean isThrowing,boolean withoutThrowingDefaultReturnValue) {
        return (t,u) -> {
            try {
                return throwingBiPredicate.test(t,u);
            } catch (Exception e) {
                log.debug("toBiPredicate isThrowing:{} withoutThrowingDefaultReturnValue:{} {} {}",isThrowing,withoutThrowingDefaultReturnValue,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return withoutThrowingDefaultReturnValue;
                }
            }
        };
    }

    static <T,U,E extends Exception> boolean test(ThrowingBiPredicate<? super T,? super U,E> throwingBiPredicate,T t,U u) {
        return toBiPredicate(throwingBiPredicate).test(t,u);
    }
    static <T,U,E extends Exception> boolean testWithoutThrowing(ThrowingBiPredicate<? super T,? super U,E> throwingBiPredicate,T t,U u,boolean withoutThrowingDefaultReturnValue) {
        return toBiPredicateWithoutThrowing(throwingBiPredicate,withoutThrowingDefaultReturnValue).test(t,u);
    }
}
