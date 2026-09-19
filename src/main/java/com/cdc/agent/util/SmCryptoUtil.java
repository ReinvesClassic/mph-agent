package com.cdc.agent.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.symmetric.SM4;

/**
 * 国密算法工具类
 * 提供 SM2（非对称加密）、SM3（摘要签名）、SM4（对称加密）能力
 */
public class SmCryptoUtil {

    private static final Logger log = LoggerFactory.getLogger(SmCryptoUtil.class);

    private SmCryptoUtil() {
    }

    // ==================== SM3 摘要/签名 ====================

    /**
     * SM3 签名（将 appSecret 与 data 拼接后做 SM3 摘要）
     *
     * @param data      待签名数据
     * @param appSecret 密钥
     * @return 签名结果（Hex 编码）
     */
    public static String sm3Sign(String data, String appSecret) {
        String content = appSecret + data;
        return SmUtil.sm3().digestHex(content);
    }

    /**
     * SM3 签名验证
     *
     * @param data      原始数据
     * @param appSecret 密钥
     * @param sign      签名值（Hex 编码）
     * @return 是否验证通过
     */
    public static boolean sm3Verify(String data, String appSecret, String sign) {
        String expected = sm3Sign(data, appSecret);
        return expected.equalsIgnoreCase(sign);
    }

    // ==================== SM2 非对称加密 ====================

    /**
     * 生成 SM2 密钥对
     *
     * @return Map 包含 privateKey（Hex）和 publicKey（Hex）
     */
    public static Map<String, String> generateSm2KeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
            generator.initialize(new ECGenParameterSpec("sm2p256v1"));
            KeyPair keyPair = generator.generateKeyPair();

            Map<String, String> result = new HashMap<>();
            result.put("privateKey", HexUtil.encodeHexStr(keyPair.getPrivate().getEncoded()));
            result.put("publicKey", HexUtil.encodeHexStr(keyPair.getPublic().getEncoded()));
            return result;
        } catch (Exception e) {
            log.error("[SM2] 密钥对生成失败", e);
            throw new RuntimeException("SM2 密钥对生成失败", e);
        }
    }

    /**
     * SM2 公钥加密
     *
     * @param data      明文
     * @param publicKey 公钥（Hex 编码）
     * @return 密文（Hex 编码）
     */
    public static String sm2Encrypt(String data, String publicKey) {
        SM2 sm2 = new SM2(null, publicKey);
        return sm2.encryptHex(data, KeyType.PublicKey);
    }

    /**
     * SM2 私钥解密
     *
     * @param encryptedData 密文（Hex 编码）
     * @param privateKey    私钥（Hex 编码）
     * @return 明文
     */
    public static String sm2Decrypt(String encryptedData, String privateKey) {
        SM2 sm2 = new SM2(privateKey, null);
        return new String(sm2.decrypt(encryptedData, KeyType.PrivateKey), StandardCharsets.UTF_8);
    }

    // ==================== SM4 对称加密 ====================

    /**
     * SM4 加密（ECB 模式）
     *
     * @param data 明文
     * @param key  密钥（16 字节）
     * @return 密文（Hex 编码）
     */
    public static String sm4Encrypt(String data, String key) {
        SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "SM4");
        SM4 sm4 = new SM4(Mode.ECB, Padding.PKCS5Padding, secretKey);
        return sm4.encryptHex(data);
    }

    /**
     * SM4 解密（ECB 模式）
     *
     * @param encryptedData 密文（Hex 编码）
     * @param key           密钥（16 字节）
     * @return 明文
     */
    public static String sm4Decrypt(String encryptedData, String key) {
        SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "SM4");
        SM4 sm4 = new SM4(Mode.ECB, Padding.PKCS5Padding, secretKey);
        return sm4.decryptStr(encryptedData);
    }
}
