package com.cdc.agent.dto;

import lombok.Data;

/**
 * 创建客户端请求
 */
@Data
public class CreateAppClientRequest {

    /** 应用名称 */
    private String appName;

    /** 备注 */
    private String remark;
}
