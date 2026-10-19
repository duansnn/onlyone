package com.owner.system.mall.support.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地磁盘存储：文件落在 {@code owner.mall.upload.dir}（默认 {@code ./uploads}），
 * 通过 {@code /files/**} 静态映射对外提供访问。
 *
 * <p>之所以按「业务目录 / 年 / 月 / 日 / 随机名」分目录，是为了避免单目录文件过多导致
 * 文件系统变慢，同时保留按天归档、按天清理的能力。</p>
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 上传根目录的绝对路径 */
    private final Path root;
    /** 对外访问前缀，不带结尾斜杠 */
    private final String publicPrefix;

    public LocalFileStorage(@Value("${owner.mall.upload.dir:./uploads}") String dir,
                            @Value("${owner.mall.upload.public-prefix:/files}") String prefix) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        this.publicPrefix = normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录：" + root, e);
        }
        log.info("本地上传目录：{}（访问前缀 {}）", root, this.publicPrefix);
    }

    /** 上传根目录，供静态资源映射使用 */
    public Path root() {
        return root;
    }

    /** 对外访问前缀，供静态资源映射使用 */
    public String publicPrefix() {
        return publicPrefix;
    }

    @Override
    public StoredFile store(InputStream in, String originalName, String contentType, String folder) throws IOException {
        String bizFolder = sanitizeFolder(folder);
        String dateDir = LocalDate.now().format(DATE_DIR);
        String ext = extensionOf(originalName, contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path targetDir = root.resolve(bizFolder).resolve(dateDir).normalize();
        if (!targetDir.startsWith(root)) {
            // 防御目录穿越：folder 已过滤，这里是兜底
            throw new IOException("非法的存储目录：" + folder);
        }
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(filename);
        long size = Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

        String url = publicPrefix + "/" + bizFolder + "/" + dateDir + "/" + filename;
        log.debug("文件已保存 {}（{} 字节）", target, size);
        return new StoredFile(url, size);
    }

    private static String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "misc";
        }
        String cleaned = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        return cleaned.isEmpty() ? "misc" : cleaned;
    }

    /** 优先用原文件名后缀，其次按 MIME 兜底，最后默认 .jpg */
    private static String extensionOf(String originalName, String contentType) {
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                String ext = originalName.substring(dot).toLowerCase(Locale.ROOT);
                if (ext.matches("\\.[a-z0-9]{1,5}")) {
                    return ext;
                }
            }
        }
        if (contentType != null) {
            return switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };
        }
        return ".jpg";
    }
}
