package com.advertisementdesign.back.common.storage.config;

import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private StorageProvider provider = StorageProvider.LOCAL;
    private String localRoot = "uploads";
    private long maxFileSize = 20L * 1024 * 1024;
    private final Oss oss = new Oss();

    @Data
    public static class Oss {
        private boolean enabled;
        private String endpoint;
        private String region;
        private String accessKeyId;
        private String accessKeySecret;
        private String publicBucket;
        private String privateBucket;
        private String publicDomain;
        private Duration signedUrlExpiration = Duration.ofMinutes(15);
    }
}
