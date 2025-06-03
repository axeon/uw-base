CREATE TABLE `sys_seq`
(
    `seq_name`      varchar(200) NOT NULL,
    `seq_id`        bigint   DEFAULT NULL,
    `seq_desc`      varchar(200) DEFAULT NULL,
    `increment_num` int      DEFAULT NULL,
    `create_date`   datetime(3)  DEFAULT NULL,
    `last_update`   datetime(3)  DEFAULT NULL,
    PRIMARY KEY (`seq_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
