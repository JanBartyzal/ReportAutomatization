package com.reportplatform.excelsync.connector;

import com.reportplatform.excelsync.config.ExcelSyncProperties;
import com.reportplatform.excelsync.model.entity.ExportFlowDefinitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;

@Component
public class LocalPathWriter implements FileConnector {

    private static final Logger log = LoggerFactory.getLogger(LocalPathWriter.class);

    private final ExcelSyncProperties properties;

    public LocalPathWriter(ExcelSyncProperties properties) {
        this.properties = properties;
    }

    @Override
    public byte[] download(ExportFlowDefinitionEntity flow, String fileName) {
        Path filePath = resolveAndValidatePath(flow.getTargetPath(), fileName);
        try {
            if (!Files.exists(filePath)) {
                log.info("File does not exist at [{}], will create new", filePath);
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    @Override
    public void upload(ExportFlowDefinitionEntity flow, byte[] content, String fileName) {
        Path filePath = resolveAndValidatePath(flow.getTargetPath(), fileName);
        Path parentDir = filePath.getParent();

        try {
            // Create parent directories if needed
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("Created parent directories: {}", parentDir);
            }

            // Attempt atomic write with file locking and retry
            writeWithLock(filePath, content);
            log.info("Successfully wrote {} bytes to {}", content.length, filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    @Override
    public boolean testConnection(ExportFlowDefinitionEntity flow) {
        try {
            Path dirPath = Path.of(flow.getTargetPath());
            validatePathInWhitelist(dirPath);
            if (Files.exists(dirPath)) {
                return Files.isWritable(dirPath);
            }
            // Check parent directory writability
            Path parent = dirPath.getParent();
            return parent != null && Files.exists(parent) && Files.isWritable(parent);
        } catch (Exception e) {
            log.warn("Connection test failed for path [{}]: {}", flow.getTargetPath(), e.getMessage());
            return false;
        }
    }

    private Path resolveAndValidatePath(String targetPath, String fileName) {
        Path dirPath = Path.of(targetPath);
        Path filePath = dirPath.resolve(fileName);
        validatePathInWhitelist(filePath);
        return filePath;
    }

    private void validatePathInWhitelist(Path path) {
        String normalizedPath = path.toAbsolutePath().normalize().toString();
        boolean allowed = properties.getAllowedPaths().stream()
                .anyMatch(allowedBase -> normalizedPath.startsWith(
                        Path.of(allowedBase).toAbsolutePath().normalize().toString()));

        if (!allowed) {
            throw new SecurityException(
                    "Path [" + normalizedPath + "] is outside allowed directories: " + properties.getAllowedPaths());
        }
    }

    private void writeWithLock(Path filePath, byte[] content) throws IOException {
        int retries = properties.getLockRetryCount();
        long retryIntervalMs = properties.getLockRetryIntervalMs();

        // Atomic write: write to temp file, then rename
        Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                Files.write(tempFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // Try to acquire lock on the target file (if it exists)
                if (Files.exists(filePath)) {
                    try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
                        FileLock lock = channel.tryLock();
                        if (lock == null) {
                            if (attempt < retries) {
                                log.warn("File [{}] is locked, retry {}/{}", filePath, attempt, retries);
                                Thread.sleep(retryIntervalMs);
                                continue;
                            }
                            throw new IOException("File is locked and retry limit exhausted: " + filePath);
                        }
                        lock.release();
                    }
                }

                // Atomic move
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (IOException e) {
                // Clean up temp file on failure
                Files.deleteIfExists(tempFile);
                if (attempt == retries) {
                    throw e;
                }
                log.warn("Write attempt {}/{} failed for [{}]: {}", attempt, retries, filePath, e.getMessage());
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry", ie);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Files.deleteIfExists(tempFile);
                throw new IOException("Interrupted during lock retry", e);
            }
        }
    }
}
