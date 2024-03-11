package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingSupplier<T,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingSupplier.class);

    T get() throws E;

    static <T,E extends Exception> Supplier<T> toSupplier(ThrowingSupplier<? extends T,E> throwingSupplier) {
        return toSupplier(throwingSupplier,true);
    }
    static <T,E extends Exception> Supplier<T> toSupplierWithoutThrowing(ThrowingSupplier<? extends T,E> throwingSupplier) {
        return toSupplier(throwingSupplier,false);
    }

    static <T,E extends Exception> Supplier<T> toSupplier(ThrowingSupplier<? extends T,E> throwingSupplier,boolean isThrowing) {
        return () -> {
            try {
                return throwingSupplier.get();
            } catch (Exception e) {
                log.debug("toSupplier isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                    return null;
                }
            }
        };
    }

    static <T,E extends Exception> T get(ThrowingSupplier<? extends T,E> throwingSupplier) {
        return toSupplier(throwingSupplier).get();
    }
    static <T,E extends Exception> T getWithoutThrowing(ThrowingSupplier<? extends T,E> throwingSupplier) {
        return toSupplierWithoutThrowing(throwingSupplier).get();
    }
}
