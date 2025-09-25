package com.alinesno.infra.data.scheduler.spark.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class StorageUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private StorageUtils() { /* no instantiation */ }

    public static Path baseDir() {
        return Paths.get(System.getProperty("user.home"), ".spark-sql-api");
    }

    public static Path tasksDir() throws IOException {
        Path p = baseDir().resolve("tasks");
        Files.createDirectories(p);
        return p;
    }

    public static Path resultsDir() throws IOException {
        Path p = baseDir().resolve("results");
        Files.createDirectories(p);
        return p;
    }

    /**
     * 原子保存 JSON 到指定文件（先写临时文件，再移动替换）
     */
    public static void saveJson(Path file, Object obj) throws IOException {
        ensureParentExists(file);
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        // 写入临时文件
        try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            MAPPER.writeValue(os, obj);
        }
        // 原子替换目标文件；若不支持原子移动，则回退到普通替换
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 原子保存为 gzip 压缩的 JSON（用于小结果）
     */
    public static void saveGzipJson(Path file, Object obj) throws IOException {
        ensureParentExists(file);
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             GZIPOutputStream gos = new GZIPOutputStream(os)) {
            MAPPER.writeValue(gos, obj);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 读取 JSON 并转换为指定类型；如果文件不存在或为空，将抛出 IOException
     */
    public static <T> T readJson(Path file, Class<T> clazz) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("file not exists: " + file);
        }
        long size = Files.size(file);
        if (size == 0) {
            throw new IOException("file is empty: " + file);
        }
        try (InputStream is = Files.newInputStream(file)) {
            return MAPPER.readValue(is, clazz);
        }
    }

    /**
     * 读取 gzip 压缩的 JSON 文件
     */
    public static <T> T readGzipJson(Path file, Class<T> clazz) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("file not exists: " + file);
        }
        long size = Files.size(file);
        if (size == 0) {
            throw new IOException("file is empty: " + file);
        }
        try (InputStream fis = Files.newInputStream(file);
             GZIPInputStream gis = new GZIPInputStream(fis)) {
            return MAPPER.readValue(gis, clazz);
        }
    }

    /**
     * 安全读取，失败或文件为空时返回 Optional.empty()
     */
    public static <T> Optional<T> readJsonOptional(Path file, Class<T> clazz) {
        try {
            return Optional.of(readJson(file, clazz));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static void ensureParentExists(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}