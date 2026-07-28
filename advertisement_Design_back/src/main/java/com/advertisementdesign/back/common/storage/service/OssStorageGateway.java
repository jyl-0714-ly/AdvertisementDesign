package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.storage.config.StorageProperties;
import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import com.advertisementdesign.back.common.storage.enums.StorageVisibility;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.storage.oss", name = "enabled", havingValue = "true")
public class OssStorageGateway implements StorageGateway {
    private final StorageProperties.Oss properties;
    private final OSS client;

    public OssStorageGateway(StorageProperties storageProperties) {
        this.properties = storageProperties.getOss();
        requireConfigured(properties.getEndpoint(), "endpoint");
        requireConfigured(properties.getAccessKeyId(), "access key id");
        requireConfigured(properties.getAccessKeySecret(), "access key secret");
        requireConfigured(properties.getPublicBucket(), "public bucket");
        requireConfigured(properties.getPrivateBucket(), "private bucket");
        requireConfigured(properties.getPublicDomain(), "public domain");
        this.client = new OSSClientBuilder().build(
                properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.OSS;
    }

    @Override
    public String bucketName(StorageVisibility visibility) {
        return visibility == StorageVisibility.PUBLIC
                ? properties.getPublicBucket() : properties.getPrivateBucket();
    }

    @Override
    public void store(String bucketName, String objectKey, InputStream inputStream, long contentLength,
                      String contentType, StorageVisibility visibility) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }
        client.putObject(bucketName, objectKey, inputStream, metadata);
    }

    @Override
    public byte[] read(String bucketName, String objectKey) throws IOException {
        try (InputStream inputStream = client.getObject(bucketName, objectKey).getObjectContent()) {
            return inputStream.readAllBytes();
        }
    }

    @Override
    public void delete(String bucketName, String objectKey) {
        client.deleteObject(bucketName, objectKey);
    }

    @Override
    public Optional<String> publicUrl(String bucketName, String objectKey, StorageVisibility visibility) {
        if (visibility != StorageVisibility.PUBLIC) {
            return Optional.empty();
        }
        String domain = properties.getPublicDomain();
        if (!StringUtils.hasText(domain)) {
            return Optional.empty();
        }
        String normalizedDomain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        return Optional.of(normalizedDomain + "/" + encodeObjectKey(objectKey));
    }

    @PreDestroy
    public void shutdown() {
        client.shutdown();
    }

    private static String encodeObjectKey(String objectKey) {
        return String.join("/", java.util.Arrays.stream(objectKey.split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .toList());
    }

    private static void requireConfigured(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("OSS " + fieldName + " must be configured when OSS is enabled");
        }
    }
}
