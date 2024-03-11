
CREATE TABLE IF NOT EXISTS tx_demo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(24) not NULL DEFAULT '' COMMENT '账号',
    customer_id BIGINT NOT NULL DEFAULT 0 COMMENT '客户号',
    status TINYINT not null default 0 comment '状态',
    amount DECIMAL(18,2) not NULL DEFAULT 0 COMMENT '金额',
    remark VARCHAR(128) not NULL DEFAULT '' COMMENT '备注',
    created DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    unique index idx_tx_demo_account_number(account_number))
    ENGINE = InnoDB
    COMMENT = 'tx_demo';
