package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.storage.config.StorageProperties;
import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import com.advertisementdesign.back.common.storage.enums.StorageVisibility;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Component
public class LocalFileStorage implements StorageGateway {
    private final Path rootDirectory;

    public LocalFileStorage(StorageProperties properties) {
        this.rootDirectory = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.LOCAL;
    }

    public void store(String objectKey, MultipartFile file) throws IOException {
        store(null, objectKey, file.getInputStream(), file.getSize(), file.getContentType(), StorageVisibility.PRIVATE);
    }

    public InputStream openStream(String objectKey) throws IOException {
        return openStream(null, objectKey);
    }

    public void delete(String objectKey) throws IOException {
        delete(null, objectKey);
    }

    @Override
    public void store(String bucketName, String objectKey, InputStream inputStream, long contentLength,
                      String contentType, StorageVisibility visibility) throws IOException {
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());
        try (InputStream source = inputStream) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public InputStream openStream(String bucketName, String objectKey) throws IOException {
        return Files.newInputStream(resolve(objectKey));
    }

    @Override
    public void delete(String bucketName, String objectKey) throws IOException {
        Files.deleteIfExists(resolve(objectKey));
    }

    @Override
    public Optional<String> publicUrl(String bucketName, String objectKey, StorageVisibility visibility) {
        return Optional.empty();
    }

    private Path resolve(String objectKey) {
        Path target = rootDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("Invalid storage object key");
        }
        return target;
    }
}
