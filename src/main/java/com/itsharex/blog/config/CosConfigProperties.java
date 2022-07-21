package com.itsharex.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 cos 配置属性
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "upload.cos")
public class CosConfigProperties {

    private String url;

    private String secretId;

    private String secretKey;

    private String region;

    private String bucketName;

}