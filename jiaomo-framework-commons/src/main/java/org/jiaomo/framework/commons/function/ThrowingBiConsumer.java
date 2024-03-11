package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingBiConsumer<T,U,E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingBiConsumer.class);

    void accept(T t,U u) throws E;

    static <T,U,E extends Exception> BiConsumer<T,U> toBiConsumer(ThrowingBiConsumer<? super T,? super U,E> throwingBiConsumer) {
        return toBiConsumer(throwingBiConsumer,true);
    }
    static <T,U,E extends Exception> BiConsumer<T,U> toBiConsumerWithoutThrowing(ThrowingBiConsumer<? super T,? super U,E> throwingBiConsumer) {
        return toBiConsumer(throwingBiConsumer,false);
    }

    static <T,U,E extends Exception> BiConsumer<T,U> toBiConsumer(ThrowingBiConsumer<? super T,? super U,E> throwingBiConsumer,boolean isThrowing) {
        return (t,u) -> {
            try {
                throwingBiConsumer.accept(t,u);
            } catch (Exception e) {
                log.debug("toBiConsumer isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
                } else {
                    log.trace(e.getMessage(),e);
                }
            }
        };
    }

    static <T,U,E extends Exception> void accept(ThrowingBiConsumer<? super T,? super U,E> throwingBiConsumer,T t,U u) {
        toBiConsumer(throwingBiConsumer).accept(t,u);
    }
    static <T,U,E extends Exception> void acceptWithoutThrowing(ThrowingBiConsumer<? super T,? super U,E> throwingBiConsumer,T t,U u) {
        toBiConsumerWithoutThrowing(throwingBiConsumer).accept(t,u);
    }
}
