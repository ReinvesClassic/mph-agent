package com.cdc.agent.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端信息 DTO（对外暴露，不包含敏感密钥）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppClientDTO {

    private Long id;
    private String appId;
    private String appName;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
