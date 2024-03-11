
CREATE TABLE IF NOT EXISTS tx_context (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tx_code VARCHAR(24) not NULL DEFAULT '' COMMENT '事务号',
    root_tx_code VARCHAR(24) not NULL DEFAULT '' COMMENT '根事务号',
    parent_tx_code VARCHAR(24) not NULL DEFAULT '' COMMENT '父事务号',
    tx_status TINYINT not null default 0 comment '事务状态',
    unit_code VARCHAR(32) not NULL DEFAULT '' COMMENT '单元号',
    global_name VARCHAR(128) not NULL DEFAULT '' COMMENT '全局事务名称',
    autonomous_name VARCHAR(128) not NULL DEFAULT '' COMMENT '自治事务名称',
    arguments_serial TEXT COMMENT '调用参数',
    context_serial TEXT COMMENT '自治事务上下文',
    created DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    unique index idx_tx_context_tx_code(tx_code),
    index idx_tx_context_root_parent_tx_code(root_tx_code,parent_tx_code),
    index idx_tx_context_tx_status_created(tx_status,created))
    ENGINE = InnoDB
    COMMENT = '事务登记表';

CREATE TABLE IF NOT EXISTS tx_global (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) not NULL DEFAULT '' COMMENT '全局事务名称',
    timeout_mills int not NULL DEFAULT 0 COMMENT '事务超时时间(单位千分之一秒)',
    propagation int not NULL DEFAULT 0 COMMENT '事务传播行为',
    rollback_for_name varchar(256) not null default '' comment '回滚条件',
    application_name VARCHAR(128) not NULL DEFAULT '' COMMENT '应用名称',
    created DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    unique index idx_tx_global_name(name))
    ENGINE = InnoDB
    COMMENT = '分布式事务定义表';

CREATE TABLE IF NOT EXISTS tx_autonomous (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) not NULL DEFAULT '' COMMENT '自治事务名称',
    commit_method VARCHAR(128) not NULL DEFAULT '' COMMENT '提交时调用的方法名称',
    rollback_method VARCHAR(128) not NULL DEFAULT '' COMMENT '回滚时调用的方法名称',
    commit_async tinyint not null default 1 comment '是否异步调用提交的方法',
    rollback_async tinyint not null default 0 comment '是否异步调用回滚的方法',
    application_name VARCHAR(128) not NULL DEFAULT '' COMMENT '应用名称',
    bean_name VARCHAR(128) not NULL DEFAULT '' COMMENT 'beanName',
    target_class_name VARCHAR(256) not NULL DEFAULT '' COMMENT '自治事务所在类的名称',
    parameter_types_name VARCHAR(512) not NULL DEFAULT '' COMMENT '自治事务所调用方法的参数类型',
    created DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    unique index idx_tx_autonomous_name(name))
    ENGINE = InnoDB
    COMMENT = '自治事务(saga/tcc)定义表';
