package com.owner.system.mall.support.file;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储抽象。
 *
 * <p>当前只有本地磁盘实现 {@link LocalFileStorage}；将来接对象存储（阿里云 OSS / 腾讯云 COS /
 * 七牛 / Cloudflare R2）时，新增一个 {@code OssFileStorage} 实现本接口并替换 Bean 即可，
 * 上传接口、前端代码和数据库里的图片字段都不需要改动。</p>
 *
 * <p>实现方只负责「落盘 + 返回可访问的 URL」，不做业务校验（类型、大小由调用方判断）。</p>
 */
public interface FileStorage {

    /**
     * 保存一个文件。
     *
     * @param in           文件内容流（由调用方负责关闭）
     * @param originalName 原始文件名，仅用于推断扩展名
     * @param contentType  MIME 类型，原始文件名没有扩展名时用它兜底
     * @param folder       业务目录，例如 {@code product}、{@code shop}
     * @return 可被浏览器直接访问的 URL（相对路径或完整 URL）与实际写入字节数
     */
    StoredFile store(InputStream in, String originalName, String contentType, String folder) throws IOException;

    /**
     * 存储结果。
     *
     * @param url  可访问地址，例如 {@code /files/product/2026/10/19/xxx.jpg}
     * @param size 实际写入的字节数
     */
    record StoredFile(String url, long size) {
    }
}
