SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;
-- agent DDL
CREATE DATABASE `agent`
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;;
use `agent`;
-- agent.chat_message DDL
CREATE TABLE `agent`.`chat_message` (`id` BIGINT NOT NULL AUTO_INCREMENT,
`memory_id` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL Comment "会话ID（LangChain4j 记忆存储键）",
`conversation_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL Comment "会话ID（关联 conversation 表）",
`role` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL Comment "USER/AI/SYSTEM",
`content` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
`token_count` INT NULL Comment "token数量",
`create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
`update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
INDEX `idx_memory_id`(`memory_id` ASC) USING BTREE,
INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
PRIMARY KEY (`id`)) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- agent.conversation DDL
CREATE TABLE `agent`.`conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话唯一ID (UUID)',
  `user_id` VARCHAR(128) NOT NULL COMMENT '用户ID',
  `title` VARCHAR(256) NULL COMMENT '会话标题',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `last_active_time` DATETIME NULL COMMENT '最后活跃时间，用于判断会话是否过期',
  INDEX `idx_user_id`(`user_id` ASC),
  UNIQUE INDEX `uk_conversation_id`(`conversation_id` ASC),
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
