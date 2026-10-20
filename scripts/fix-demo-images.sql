-- 演示数据修复：把指向 example.com 的假图片地址换成站内真实图片
--
-- 背景：scripts/ 下的冒烟脚本不需要真实图片，一直用 https://example.com/img/xxx.jpg，
-- 这些地址在浏览器里必然 404，于是店面、评价、后台表格里全是「暂无图片」占位。
-- 真实商品图放在 owner-web/public/demo/p1..p10.jpg（纸巾 / 清洁剂 / 垃圾袋 / 洗碗布 /
-- 保鲜盒 / 厨具 / 收纳盒 / 粘钩 / 洗手液 / 牙刷），这里按商品所属二级类目对号入座。
--
-- 幂等：只动 example.com 与空值，跑多少次都不会覆盖真实上传的 /files/** 图片。
-- 用法：mysql -h127.0.0.1 -uroot owner < scripts/fix-demo-images.sql

-- 1) 商品图：按二级类目名映射到对应的演示图
UPDATE product_image pi
JOIN product p ON p.id = pi.product_id
LEFT JOIN category c ON c.id = p.category_id
SET pi.url = CASE c.name
        WHEN '纸巾' THEN '/demo/p1.jpg'
        WHEN '清洁剂' THEN '/demo/p2.jpg'
        WHEN '垃圾袋' THEN '/demo/p3.jpg'
        WHEN '餐具' THEN '/demo/p4.jpg'
        WHEN '保鲜盒' THEN '/demo/p5.jpg'
        WHEN '厨具小件' THEN '/demo/p6.jpg'
        WHEN '收纳盒' THEN '/demo/p7.jpg'
        WHEN '挂钩' THEN '/demo/p8.jpg'
        WHEN '洗护' THEN '/demo/p9.jpg'
        WHEN '口腔' THEN '/demo/p10.jpg'
        ELSE '/demo/p1.jpg'
    END
WHERE pi.url LIKE '%example.com%'
   OR pi.url IS NULL
   OR pi.url = '';

-- 2) 评价晒图：冒烟脚本统一用 r1.jpg，这里统一换成纸巾图
UPDATE review
SET image_urls = '/demo/p1.jpg'
WHERE image_urls LIKE '%example.com%';

-- 3) 核对：应当都为 0
SELECT
    (SELECT COUNT(*) FROM product_image WHERE url LIKE '%example.com%') AS bad_product_images,
    (SELECT COUNT(*) FROM review WHERE image_urls LIKE '%example.com%') AS bad_review_images;
