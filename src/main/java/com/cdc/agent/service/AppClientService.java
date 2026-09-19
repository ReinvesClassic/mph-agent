package com.cdc.agent.service;

import com.cdc.agent.dto.AppClientDTO;
import com.cdc.agent.dto.AppClientSecretDTO;
import com.cdc.agent.entity.AppClientEntity;
import com.cdc.agent.exception.AgentException;
import com.cdc.agent.repository.AppClientRepository;
import com.cdc.agent.util.SmCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API 客户端管理服务
 * 负责客户端的增删改查及签名验证
 */
@Service
public class AppClientService {

    private static final Logger log = LoggerFactory.getLogger(AppClientService.class);

    private final AppClientRepository appClientRepository;

    public AppClientService(AppClientRepository appClientRepository) {
        this.appClientRepository = appClientRepository;
    }

    /**
     * 创建客户端（自动生成 appId、appSecret、SM2 密钥对）
     * 敏感信息仅在创建时返回一次
     */
    @Transactional
    public AppClientSecretDTO createClient(String appName, String remark) {
        String appId = generateAppId();
        String appSecret = generateAppSecret();
        Map<String, String> sm2Keys = SmCryptoUtil.generateSm2KeyPair();

        AppClientEntity entity = new AppClientEntity();
        entity.setAppId(appId);
        entity.setAppSecret(appSecret);
        entity.setAppName(appName);
        entity.setSm2PrivateKey(sm2Keys.get("privateKey"));
        entity.setSm2PublicKey(sm2Keys.get("publicKey"));
        entity.setEnabled(true);
        entity.setRemark(remark);

        appClientRepository.save(entity);
        log.info("[Auth] 创建客户端: appId={}, appName={}", appId, appName);

        return new AppClientSecretDTO(appId, appSecret,
                sm2Keys.get("privateKey"), sm2Keys.get("publicKey"));
    }

    /**
     * 查询所有客户端（不返回敏感密钥）
     */
    public List<AppClientDTO> listClients() {
        return appClientRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据 appId 查询客户端
     */
    public AppClientDTO getClient(String appId) {
        AppClientEntity entity = appClientRepository.findByAppId(appId)
                .orElseThrow(() -> AgentException.notFound("客户端不存在: " + appId));
        return toDTO(entity);
    }

    /**
     * 启用/禁用客户端
     */
    @Transactional
    public AppClientDTO toggleEnabled(String appId, boolean enabled) {
        AppClientEntity entity = appClientRepository.findByAppId(appId)
                .orElseThrow(() -> AgentException.notFound("客户端不存在: " + appId));
        entity.setEnabled(enabled);
        appClientRepository.save(entity);
        log.info("[Auth] {} 客户端: appId={}", enabled ? "启用" : "禁用", appId);
        return toDTO(entity);
    }

    /**
     * 删除客户端
     */
    @Transactional
    public void deleteClient(String appId) {
        if (!appClientRepository.existsByAppId(appId)) {
            throw AgentException.notFound("客户端不存在: " + appId);
        }
        appClientRepository.deleteByAppId(appId);
        log.info("[Auth] 删除客户端: appId={}", appId);
    }

    /**
     * 重置客户端密钥（重新生成 appSecret 和 SM2 密钥对）
     */
    @Transactional
    public AppClientSecretDTO resetSecret(String appId) {
        AppClientEntity entity = appClientRepository.findByAppId(appId)
                .orElseThrow(() -> AgentException.notFound("客户端不存在: " + appId));

        String newSecret = generateAppSecret();
        Map<String, String> sm2Keys = SmCryptoUtil.generateSm2KeyPair();

        entity.setAppSecret(newSecret);
        entity.setSm2PrivateKey(sm2Keys.get("privateKey"));
        entity.setSm2PublicKey(sm2Keys.get("publicKey"));
        appClientRepository.save(entity);

        log.info("[Auth] 重置密钥: appId={}", appId);
        return new AppClientSecretDTO(appId, newSecret,
                sm2Keys.get("privateKey"), sm2Keys.get("publicKey"));
    }

    /**
     * 根据 appId 查找客户端实体（内部使用）
     */
    public AppClientEntity findByAppId(String appId) {
        return appClientRepository.findByAppId(appId).orElse(null);
    }

    /**
     * 验证 SM3 签名
     *
     * @param data      待验证数据
     * @param appSecret 客户端密钥
     * @param sign      签名值
     * @return 是否通过
     */
    public boolean verifySign(String data, String appSecret, String sign) {
        return SmCryptoUtil.sm3Verify(data, appSecret, sign);
    }

    /**
     * 确保默认客户端存在（由 AuthConfig 启动时调用）
     */
    @Transactional
    public void ensureDefaultClient(String appId, String appSecret, String appName) {
        if (!appClientRepository.existsByAppId(appId)) {
            Map<String, String> sm2Keys = SmCryptoUtil.generateSm2KeyPair();

            AppClientEntity entity = new AppClientEntity();
            entity.setAppId(appId);
            entity.setAppSecret(appSecret);
            entity.setAppName(appName);
            entity.setSm2PrivateKey(sm2Keys.get("privateKey"));
            entity.setSm2PublicKey(sm2Keys.get("publicKey"));
            entity.setEnabled(true);
            entity.setRemark("系统默认客户端");

            appClientRepository.save(entity);
            log.info("[Auth] 初始化默认客户端: appId={}", appId);
        }
    }

    private AppClientDTO toDTO(AppClientEntity entity) {
        return new AppClientDTO(
                entity.getId(),
                entity.getAppId(),
                entity.getAppName(),
                entity.getEnabled(),
                entity.getRemark(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private String generateAppId() {
        return "app_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateAppSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
