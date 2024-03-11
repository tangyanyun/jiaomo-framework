package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingConsumer<T,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingConsumer.class);

    void accept(T t) throws E;

    static <T,E extends Exception> Consumer<T> toConsumer(ThrowingConsumer<? super T,E> throwingConsumer) {
        return toConsumer(throwingConsumer,true);
    }
    static <T,E extends Exception> Consumer<T> toConsumerWithoutThrowing(ThrowingConsumer<? super T,E> throwingConsumer) {
        return toConsumer(throwingConsumer,false);
    }

    static <T,E extends Exception> Consumer<T> toConsumer(ThrowingConsumer<? super T,E> throwingConsumer,boolean isThrowing) {
        return t -> {
            try {
                throwingConsumer.accept(t);
            } catch (Exception e) {
                log.debug("toConsumer isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                }
            }
        };
    }

    static <T,E extends Exception> void accept(ThrowingConsumer<? super T,E> throwingConsumer,T t) {
        toConsumer(throwingConsumer).accept(t);
    }
    static <T,E extends Exception> void acceptWithoutThrowing(ThrowingConsumer<? super T,E> throwingConsumer,T t) {
        toConsumerWithoutThrowing(throwingConsumer).accept(t);
    }
}
