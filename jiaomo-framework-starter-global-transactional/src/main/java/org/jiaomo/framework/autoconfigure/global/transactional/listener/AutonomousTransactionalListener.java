package org.jiaomo.framework.autoconfigure.global.transactional.listener;

import org.jiaomo.framework.autoconfigure.global.transactional.domain.TxContext;

public interface AutonomousTransactionalListener {
    default void onStartAutonomousTransaction(TxContext.AutonomousContext autonomousContext) {
    }

    default void onEndAutonomousTransaction(TxContext.AutonomousContext autonomousContext) {
    }

    default void onAutonomousTransactionCommit(TxContext.AutonomousContext autonomousContext) {
    }
    default void onAutonomousTransactionCommitComplete(TxContext.AutonomousContext autonomousContext) {
    }

    default void onAutonomousTransactionCommitException(TxContext.AutonomousContext autonomousContext,Throwable throwable) {
    }

    default void onAutonomousTransactionRollback(TxContext.AutonomousContext autonomousContext) {
    }

    default void onAutonomousTransactionRollbackComplete(TxContext.AutonomousContext autonomousContext) {

    }

    default void onAutonomousTransactionRollbackException(TxContext.AutonomousContext autonomousContext,Throwable throwable) {
    }
}
