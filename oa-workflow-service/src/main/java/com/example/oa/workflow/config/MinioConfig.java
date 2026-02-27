package com.example.oa.workflow.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。
 */
@Configuration
public class MinioConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.minio")
    public MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean
    public MinioClient minioClient(MinioProperties p) {
        return MinioClient.builder()
                .endpoint(p.getEndpoint())
                .credentials(p.getAccessKey(), p.getSecretKey())
                .build();
    }

    @Data
    public static class MinioProperties {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "oa-attachments";
    }
}
