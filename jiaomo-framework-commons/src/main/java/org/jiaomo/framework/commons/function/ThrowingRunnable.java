package org.jiaomo.framework.commons.function;

import org.jiaomo.framework.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {
    static final Logger log = LoggerFactory.getLogger(ThrowingRunnable.class);

    void run() throws E;

    static <E extends Exception> Runnable toRunnable(ThrowingRunnable<E> throwingRunnable) {
        return toRunnable(throwingRunnable,true);
    }
    static <E extends Exception> Runnable toRunnableWithoutThrowing(ThrowingRunnable<E> throwingRunnable) {
        return toRunnable(throwingRunnable,false);
    }

    static <E extends Exception> Runnable toRunnable(ThrowingRunnable<E> throwingRunnable,boolean isThrowing) {
        return () -> {
            try {
                throwingRunnable.run();
            } catch (Exception e) {
                log.debug("toRunnable isThrowing:{} {} {}",isThrowing,e.getClass().getName(),e.getMessage());
                if (isThrowing) {
                    throw e instanceof BusinessException ? (BusinessException)e : new BusinessException(e);
//                  ThrowingFunction.<RuntimeException>sneakyThrow(e);
                } else {
                    log.trace(e.getMessage(),e);
                }
            }
        };
    }

    static <E extends Exception> void run(ThrowingRunnable<E> throwingRunnable) {
        toRunnable(throwingRunnable).run();
    }
    static <E extends Exception> void runWithoutThrowing(ThrowingRunnable<E> throwingRunnable) {
        toRunnableWithoutThrowing(throwingRunnable).run();
    }
}
