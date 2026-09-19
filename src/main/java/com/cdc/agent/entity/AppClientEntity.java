package com.cdc.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * API 客户端实体
 * 存储授权访问的客户端信息及其国密密钥
 */
@Data
@Entity
@Table(name = "app_client")
public class AppClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 应用ID（唯一标识） */
    @Column(nullable = false, unique = true, length = 64)
    private String appId;

    /** 应用密钥（SM3 签名用） */
    @Column(nullable = false, length = 128)
    private String appSecret;

    /** 应用名称 */
    @Column(length = 128)
    private String appName;

    /** SM2 私钥（Hex 编码，客户端持有用于解密响应） */
    @Column(columnDefinition = "TEXT")
    private String sm2PrivateKey;

    /** SM2 公钥（Hex 编码，服务端持有用于加密响应） */
    @Column(columnDefinition = "TEXT")
    private String sm2PublicKey;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 备注 */
    @Column(length = 512)
    private String remark;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
