package com.cdc.agent.exception;

import lombok.Getter;

/**
 * 业务异常
 * 用于 Agent 模块中的参数校验、业务逻辑错误等场景
 */
@Getter
public class AgentException extends RuntimeException {

    private final int code;

    public AgentException(String message) {
        super(message);
        this.code = 400;
    }

    public AgentException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 参数校验失败
     */
    public static AgentException paramError(String message) {
        return new AgentException(400, message);
    }

    /**
     * 资源不存在
     */
    public static AgentException notFound(String message) {
        return new AgentException(404, message);
    }

    /**
     * 无权限
     */
    public static AgentException forbidden(String message) {
        return new AgentException(403, message);
    }

    /**
     * 服务内部错误
     */
    public static AgentException internalError(String message) {
        return new AgentException(500, message);
    }
}
