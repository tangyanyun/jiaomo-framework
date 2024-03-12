## 分布式事务组件

----------------------------------------------

### Spring数据库事务管理

```
    @Service
    public class DemoService {
    
        @Transactional
        public void transaction(String accountNumber, BigDecimal amount,long customerId) {
            ...
        }
    }
```

- Spring声明式事务管理通过 @Transactional 注解标注数据库事务
- 当被标注的方法开始执行时会开启、加入或挂起数据库事务(begin)
- 当被标注的方法抛出特定的异常(缺省RuntimeException)时数据库事务被框架自动回滚
- 当被标注的方法未抛出异常或未抛出特定的异常时数据库事务被框架自动提交

----------------------------------------------

### 分布式事务组件@GlobalTransactional

```
    @GlobalTransactional(name="DemoLocalApplicationService.tx01WithAnnotation",
            timeoutMills = 300000,
            rollbackFor = { RuntimeException.class },
            propagation = Propagation.REQUIRED)
    public void tx01WithAnnotation(String accountNumber, BigDecimal amount,long customerId) {
        demoGlobalTransactionalFacade.tccTryDSWithAnnotation(accountNumber,amount,customerId);
        demoLocalDomainService.tryTxWithAnnotation(new StringBuilder(accountNumber), amount,customerId);
    }
```

- 分布式事务组件声明式事务管理通过 @GlobalTransactional 注解标注分布式事务
- 当被标注的方法开始执行时会开启、加入或挂起分布式事务
- 当被标注的方法抛出特定的异常(缺省RuntimeException)时分布式事务被组件自动回滚
- 当被标注的方法未抛出异常或未抛出特定的异常时分布式事务被组件自动提交
- 每个分布式事务包含若干个自治事务单元(@AutonomousTransactional)

----------------------------------------------

### 分布式事务组件@AutonomousTransactional

```
    @Transactional
    @AutonomousTransactional(name="DemoGlobalTransactionalDomainService.tccTryWithAnnotation",
            commitMethod = "tccConfirm",rollbackMethod = "tccCancel")
    public void tccTryWithAnnotation(String accountNumber, BigDecimal amount, long customerId) {
        log.info("DemoGlobalTransactionalService.tccTry =======");
        jdbcTemplate.update("insert into tx_demo (account_number,amount,customer_id,status) values (?,?,?,?)",
                accountNumber,amount,customerId,(byte)0);
    }
```

- 通过 @AutonomousTransactional 注解标注分布式事务的自治事务单元
- 自治事务单元采用SAGA或tcc模式，组件支持 SAGA和tcc混合编排
- 通过参数commitMethod/rollbackMethod指定分布式事务提交阶段/回滚阶段调用的方法名称
- 当分布式事务回滚时组件自动逆序调用其下所有自治事务单元的rollbackMethod
- 当分布式事务提交时组件自动顺序调用其下所有自治事务单元的commitMethod
- 自治事务单元必须被标注在开启了的分布式事务之下，否则无效

----------------------------------------------

### @GlobalTransactional参数

```
public @interface GlobalTransactional {
    String name() default "";
    int timeoutMills() default 300000;

    Class<? extends Throwable>[] rollbackFor() default { RuntimeException.class };

    Propagation propagation() default Propagation.REQUIRED;
}
```

- name: 分布式事务名称，全系统唯一，分布式事务定义表的唯一索引
- timeoutMills: 分布式事务超时时间(单位:千分之一秒)
- rollbackFor: 触发回滚时应用程序抛出的异常类型
- propagation: 分布式事务传播行为，支持全部7种传播行为

----------------------------------------------

### 分布式事务7种传播行为

|     | 名称            | 说明  |
|-----|---------------|------|
| (1) | REQUIRED      | 支持当前事务，如果当前没有事务，就新建一个事务。(缺省) |
| (2) | REQUIRES_NEW  | 新建事务，如果当前存在事务，把当前事务挂起。 |
| (3) | NESTED        | 在分布式事务中，行为与REQUIRES_NEW相同 |
| (4) | SUPPORTS      | 支持当前事务，如果当前没有事务，就以非事务方式执行。   |
| (5) | MANDATORY     | 支持当前事务，如果当前没有事务，就抛出异常。   |
| (6) | NOT_SUPPORTED | 以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。 |
| (7) | NEVER         | 以非事务方式执行，如果当前存在事务，则抛出异常。   |

- 数据库事务的NESTED传播行为基于SavePoint技术，分布式事务无SavePoint概念，所以在分布式事务组件中，NESTED传播行为与REQUIRES_NEW相同

----------------------------------------------

### @AutonomousTransactional参数

```
public @interface AutonomousTransactional {
    String name();

    String commitMethod() default "";
    boolean commitAsync() default true;

    String rollbackMethod() default "rollback";
    boolean rollbackAsync() default false;
}
```

- name: 分布式事务中自治事务单元名称，全系统唯一，自治事务单元定义表的唯一索引
- commitMethod: 自治事务单元提交阶段调用的方法名称(SAGA模式为空)
- commitAsync: 自治事务单元提交阶段调用的方法是否异步执行(缺省true)
- rollbackMethod: 自治事务单元回滚阶段调用的方法名称
- rollbackAsync: 自治事务单元回滚阶段调用的方法是否异步执行(缺省false)

----------------------------------------------

### 自治事务上下文

```
    @AutonomousTransactional(name="DemoLocalDomainService.tryTxWithAnnotation", commitMethod = "confirmTx",rollbackMethod = "cancelTx")
    @Transactional
    public void tryTxWithAnnotation(final StringBuilder accountNumber, final BigDecimal amount,final long customerId) {
        jdbcTemplate.update("update tx_demo set amount = amount + 1 where account_number = ?",accountNumber);
        accountNumber.append("-2");
        if (TxContextHolder.getContext() != null)//当无分布式事务时,TxContextHolder.getContext()为空
            TxContextHolder.getContext().put("hello","This is a message from tryTx.");
        if (customerId == 4L) {
            throw new RuntimeException("custom invalid");
        }
    }
```

- 为了方便自治事务单元调用方法之间传递数据，组件提供了自治事务上下文
- 自治事务上下文类型为Map<String,Object>
- 自治事务上下文可以通过TxContextHolder.getContext()静态方法获取
- 例如在tcc事务模式下，try方法放入的数据可以在confirm/cancel方法中读取

----------------------------------------------

### 分布式事务与数据库事务

- 本组件管理的分布式事务与数据库事务没有关系
- 管理数据库事务，推荐使用Spring事务管理
- 本分布式事务组件不提供管理数据库事务的功能
- 分布式事务的自治事务单元执行方法可能开启数据库事务
- 服务编排组件、分布式事务组件、应用程序各自管理自己的数据库事务
- 服务编排组件、分布式事务组件、应用程序在将程序执行权移交给其它组件时需结束自己的数据库事务，避免其它组件在提交/回滚数据库事务时影响到自己的数据库事务
- 服务编排组件、分布式事务组件、应用程序不应依赖其它组件管理自己的数据库事务

----------------------------------------------

### 分布式事务的事务状态

```
public enum TxStatusEnum {
    NO_TRANSACTIONAL((byte)4,"非事务"),
    TRANSACTING((byte)10,"交易中"),
    TRANSACTED((byte)12,"已交易"),
    TRANSACT_FAILED((byte)14,"交易失败"),
    COMMITTING((byte)20,"提交中"),
    COMMITTED((byte)22,"已提交"),
    COMMIT_FAILED((byte)24,"提交失败"),
    ROLLING_BACK((byte)30,"回滚中"),
    ROLLED_BACK((byte)32,"已回滚"),
    ROLL_BACK_FAILED((byte)34,"回滚失败");
```

----------------------------------------------

### 分布式事务的故障恢复

##### 扫描数据库，发现已经超时且未达终态的分布式事务记录，对该记录做如下处理：

- 已经进入提交流程的分布式事务，继续提交流程
- 未进入提交流程的分布式事务，进行回滚

----------------------------------------------

### 分布式事务的编程式事务管理(一)

```
    public void tx51(String accountNumber, BigDecimal amount,long customerId) {
        String txCode = GlobalTransactionManager.begin("DemoLocalApplicationService.tx51");
        try {
            demoGlobalTransactionalFacade.tccTry(accountNumber, amount, customerId);
            demoLocalDomainService.tryTx(new StringBuilder(accountNumber), amount, customerId);
        } catch (RuntimeException e) {
            GlobalTransactionManager.rollback(txCode);
            throw e;
        } catch (Throwable e) {
            GlobalTransactionManager.commit(txCode);
            throw e;
        }
        GlobalTransactionManager.commit(txCode);
    }
```

----------------------------------------------

### 分布式事务的编程式事务管理(二)

```
    @Transactional
    public void tryTx(final StringBuilder accountNumber, final BigDecimal amount,final long customerId) {
        log.info("tryTx accountNumber:{} amount:{} customerId:{}",accountNumber,amount,customerId);
        accountNumber.append("-2");

        String txCode = GlobalTransactionManager.startAutonomousTransaction(this,
                "DemoLocalDomainService.tryTx","confirmTx","cancelTx",
                DemoLocalDomainService.class, Arrays.asList(StringBuilder.class, BigDecimal.class,Long.TYPE),
                accountNumber,amount,customerId);
        try {
            jdbcTemplate.update("update tx_demo set amount = amount + 1 where account_number = ?",
                    accountNumber.toString().substring(0,accountNumber.length() - 2));

            //当无分布式事务 或 使用GlobalTransactionManager.autonomousTransactional，TxContextHolder.getContext()为空
            if (TxContextHolder.getContext() != null)
                TxContextHolder.getContext().put("hello", "This is a message from tryTx.");

            if (customerId == 4L) {
                throw new RuntimeException("custom invalid");
            }
        } finally {
            GlobalTransactionManager.endAutonomousTransaction(txCode);
        }
    }
```

----------------------------------------------

### 分布式事务的编程式事务管理(三)

```
    @Transactional
    public void tryTx52(final StringBuilder accountNumber, final BigDecimal amount,final long customerId) {
        log.info("tryTx52 accountNumber:{} amount:{} customerId:{}",accountNumber,amount,customerId);
        accountNumber.append("-2");

        jdbcTemplate.update("update tx_demo set amount = amount + ? where account_number = ?",BigDecimal.ONE,accountNumber.toString().substring(0,accountNumber.length() - 2));

        if (TxContextHolder.getContext() != null)//当无分布式事务 或 使用GlobalTransactionManager.autonomousTransactional，TxContextHolder.getContext()为空
            TxContextHolder.getContext().put("hello","This is a message from tryTx.");

        GlobalTransactionManager.autonomousTransactional(this,"DemoLocalDomainService.tryTx","confirmTx","cancelTx",
                DemoLocalDomainService.class, Arrays.asList(StringBuilder.class, BigDecimal.class,Long.TYPE),accountNumber,amount,customerId);

        if (customerId == 4L) {
            throw new RuntimeException("custom invalid");
        }
    }
```

----------------------------------------------
