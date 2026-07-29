package com.advertisementdesign.back.common.storage.controller;

/**
 * Business file endpoints moved to their owning modules. Kept as a non-controller compatibility
 * marker so package references fail closed rather than exposing unscoped file-id APIs.
 */
@Deprecated(forRemoval = true)
final class FileController {
    private FileController() {
    }
}
