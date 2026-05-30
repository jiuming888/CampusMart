package com.campusmart.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class AlipayConfig {

    private static final Logger logger = LoggerFactory.getLogger(AlipayConfig.class);

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.private-key-path:}")
    private String privateKeyPath;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.gateway-url}")
    private String gatewayUrl;

    @Value("${alipay.return-url}")
    private String returnUrl;

    @Value("${alipay.notify-url}")
    private String notifyUrl;

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    @Bean
    public AlipayClient alipayClient() throws IOException {
        String actualPrivateKey = privateKey;

        // 如果配置了密钥文件路径，从文件读取
        if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
            try {
                ClassPathResource resource = new ClassPathResource(privateKeyPath);
                try (InputStream is = resource.getInputStream()) {
                    actualPrivateKey = StreamUtils.copyToString(is, StandardCharsets.UTF_8).trim();
                }
                logger.info("从文件加载私钥: {}", privateKeyPath);
            } catch (IOException e) {
                logger.error("读取私钥文件失败: {}", privateKeyPath, e);
                throw e;
            }
        }

        // 清理密钥格式（移除头尾和换行）
        actualPrivateKey = actualPrivateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // 清理支付宝公钥格式
        String cleanPublicKey = alipayPublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        logger.info("初始化支付宝客户端，AppId: {}, Gateway: {}", appId, gatewayUrl);

        return new DefaultAlipayClient(
            gatewayUrl,
            appId,
            actualPrivateKey,
            "json",
            "UTF-8",
            cleanPublicKey,
            "RSA2"
        );
    }
}
