package com.cdc.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建客户端响应（包含敏感密钥，仅在创建时返回一次）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppClientSecretDTO {

    private String appId;
    private String appSecret;

    /** SM2 私钥（Hex，客户端保存用于解密响应） */
    private String sm2PrivateKey;

    /** SM2 公钥（Hex，客户端保存用于验证服务端签名） */
    private String sm2PublicKey;
}
