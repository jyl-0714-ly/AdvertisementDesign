package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import com.advertisementdesign.back.common.storage.enums.StorageVisibility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface StorageGateway {
    StorageProvider provider();

    void store(String bucketName, String objectKey, InputStream inputStream, long contentLength,
               String contentType, StorageVisibility visibility) throws IOException;

    InputStream openStream(String bucketName, String objectKey) throws IOException;

    void delete(String bucketName, String objectKey) throws IOException;

    Optional<String> publicUrl(String bucketName, String objectKey, StorageVisibility visibility);

    default String bucketName(StorageVisibility visibility) {
        return null;
    }
}
