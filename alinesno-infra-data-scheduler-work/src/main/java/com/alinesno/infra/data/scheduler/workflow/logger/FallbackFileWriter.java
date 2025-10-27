package com.alinesno.infra.data.scheduler.workflow.logger;

import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * fallback日志写入器
 */
public class FallbackFileWriter {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();

    public FallbackFileWriter(String path) {
        this.file = Paths.get(path);
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException ignored) {}
    }

    public synchronized void write(List<NodeLog> batch) {
        try (BufferedWriter writer = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (NodeLog n : batch) {
                writer.write(mapper.writeValueAsString(n));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("FallbackFileWriter failed: " + e.getMessage());
        }
    }
}