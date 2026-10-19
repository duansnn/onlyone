package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.mall.support.file.FileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 图片上传。
 *
 * <p>登录后即可上传（店主传商品图、顾客传评价图都走这里），落盘与访问地址由
 * {@link FileStorage} 决定，当前实现为本地磁盘，URL 形如
 * {@code /files/product/2026/10/19/xxx.jpg}。</p>
 *
 * <p>不做图片压缩、裁剪、水印：这些属于图片处理能力，等接入对象存储时由服务商侧完成。</p>
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "通用-上传", description = "图片上传（本地磁盘存储，返回可直接使用的相对地址）")
public class UploadController {

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp", "image/gif");

    /** 允许的业务目录，避免出现奇怪的一级目录 */
    private static final Set<String> ALLOWED_FOLDERS = Set.of("product", "shop", "review", "avatar");

    private final FileStorage fileStorage;

    @Value("${owner.mall.upload.max-image-size:5242880}")
    private long maxImageSize;

    @Operation(summary = "上传图片", description = "multipart/form-data，字段名 file；返回图片相对地址")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> uploadImage(
            @Parameter(description = "图片文件，支持 jpg/png/webp/gif，默认上限 5 MB")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务目录：product / shop / review / avatar，默认 product")
            @RequestParam(value = "folder", required = false, defaultValue = "product") String folder) {

        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要上传的图片");
        }
        if (file.getSize() > maxImageSize) {
            throw new ServiceException("图片不能超过 " + (maxImageSize / 1024 / 1024) + " MB");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceException("只支持 jpg / png / webp / gif 格式的图片");
        }
        String bizFolder = ALLOWED_FOLDERS.contains(folder) ? folder : "product";

        try (InputStream in = file.getInputStream()) {
            FileStorage.StoredFile stored = fileStorage.store(in, file.getOriginalFilename(), contentType, bizFolder);
            return R.ok(stored.url());
        } catch (IOException e) {
            throw new ServiceException("图片保存失败，请稍后重试", e);
        }
    }
}
