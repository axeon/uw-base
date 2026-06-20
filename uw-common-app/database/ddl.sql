-- saas_mall.sys_crit_log definition

CREATE TABLE `sys_crit_log` (
                                `id` bigint NOT NULL COMMENT 'ID',
                                `saas_id` bigint NOT NULL COMMENT 'saasId',
                                `mch_id` bigint DEFAULT NULL COMMENT '商户ID',
                                `user_id` bigint NOT NULL COMMENT '用户id',
                                `user_type` int DEFAULT NULL COMMENT '用户类型',
                                `group_id` bigint DEFAULT NULL COMMENT '用户组ID',
                                `user_name` varchar(100) DEFAULT NULL COMMENT '用户名',
                                `nick_name` varchar(100) DEFAULT NULL COMMENT '用户昵称',
                                `real_name` varchar(100) DEFAULT NULL COMMENT '真实名称',
                                `user_ip` varchar(50) DEFAULT NULL COMMENT '操作人ip',
                                `api_uri` varchar(200) DEFAULT NULL COMMENT '请求uri',
                                `api_name` varchar(200) DEFAULT NULL COMMENT 'API名称',
                                `biz_type` varchar(100) DEFAULT NULL COMMENT '操作对象类型',
                                `biz_id` varchar(100) DEFAULT NULL COMMENT '操作对象id',
                                `biz_log` text COMMENT '业务日志',
                                `op_state` varchar(200) DEFAULT NULL COMMENT '操作状态',
                                `op_log` text COMMENT '日志内容',
                                `request_date` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                                `response_state` varchar(10) DEFAULT NULL COMMENT '响应状态',
                                `request_body` longtext COMMENT '请求参数',
                                `response_code` varchar(100) DEFAULT NULL COMMENT '响应代码',
                                `response_body` longtext COMMENT '响应日志',
                                `response_msg` varchar(1000) DEFAULT NULL COMMENT '响应消息',
                                `response_millis` bigint DEFAULT NULL COMMENT '请求毫秒数',
                                `exception` text COMMENT '异常信息',
                                `status_code` int DEFAULT NULL COMMENT '响应状态码',
                                `app_info` varchar(100) DEFAULT NULL COMMENT '应用信息',
                                `app_host` varchar(100) DEFAULT NULL COMMENT '应用主机',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统关键日志';


-- saas_mall.sys_data_history definition

CREATE TABLE `sys_data_history` (
                                    `id` bigint NOT NULL COMMENT 'ID',
                                    `saas_id` bigint NOT NULL COMMENT 'saasId',
                                    `mch_id` bigint DEFAULT NULL COMMENT '商户ID',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `user_type` int DEFAULT NULL COMMENT '用户类型',
                                    `group_id` bigint DEFAULT NULL COMMENT '用户的组ID',
                                    `user_name` varchar(100) DEFAULT NULL COMMENT '用户名称',
                                    `nick_name` varchar(100) DEFAULT NULL COMMENT '用户昵称',
                                    `real_name` varchar(100) DEFAULT NULL COMMENT '真实名称',
                                    `entity_class` varchar(100) DEFAULT NULL COMMENT '实体类',
                                    `entity_id` varchar(100) DEFAULT NULL COMMENT '实体ID',
                                    `entity_name` varchar(200) DEFAULT NULL COMMENT '实体名',
                                    `entity_data` json DEFAULT NULL COMMENT '实体数据',
                                    `entity_update_info` longtext COMMENT '实体修改信息',
                                    `remark` text COMMENT '备注信息',
                                    `user_ip` varchar(100) DEFAULT NULL COMMENT '用户IP',
                                    `create_date` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建日期',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统数据历史';