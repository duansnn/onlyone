-- 演示账号归位：全店只留三个账号
--   koko        超级管理员 / 店主（平台超管）
--   思柯        店员       18203536390
--   Amara       顾客       13259841768
--
-- 说明：
--   * 其余账号一律逻辑删除（del_flag=1），订单、评价等历史数据仍能正常查看，
--     只是不再出现在「店员管理」「顾客管理」列表里；
--   * 店铺成员只保留 koko 与思柯两条；
--   * 可重复执行，跑多少次结果都一样。
--
-- 用法：mysql --default-character-set=utf8mb4 -uroot -p9999 owner < scripts/reset-demo-accounts.sql

SET NAMES utf8mb4;

-- 1. 店铺成员：只留 koko 与 思柯
DELETE FROM shop_member
WHERE user_id NOT IN (
        SELECT id FROM sys_user WHERE username IN ('koko', '18203536390')
    );

-- 2. 其余账号逻辑删除
UPDATE sys_user
SET del_flag = 1,
    version  = version + 1,
    update_time = NOW()
WHERE del_flag = 0
  AND username NOT IN ('koko', '18203536390', '13259841768');

-- 3. 三个账号确保可用
UPDATE sys_user
SET del_flag = 0,
    status   = 0,
    update_time = NOW()
WHERE username IN ('koko', '18203536390', '13259841768');

-- 4. 补回两条成员关系（koko 店主 / 思柯 店员），已有就跳过
INSERT INTO shop_member (id, shop_id, user_id, role, status, del_flag, version, create_time, update_time)
SELECT 2112060000000000002, s.id, u.id, 'OWNER', 0, 0, 0, NOW(), NOW()
FROM shop s
         JOIN sys_user u ON u.username = 'koko'
WHERE s.name = '日用铺'
  AND NOT EXISTS (SELECT 1 FROM shop_member m WHERE m.shop_id = s.id AND m.user_id = u.id);

INSERT INTO shop_member (id, shop_id, user_id, role, status, del_flag, version, create_time, update_time)
SELECT 2112060000000000003, s.id, u.id, 'STAFF', 0, 0, 0, NOW(), NOW()
FROM shop s
         JOIN sys_user u ON u.username = '18203536390'
WHERE s.name = '日用铺'
  AND NOT EXISTS (SELECT 1 FROM shop_member m WHERE m.shop_id = s.id AND m.user_id = u.id);

-- 5. 校验
SELECT u.username, u.nickname, u.mobile, u.del_flag, m.role, m.status AS member_status
FROM sys_user u
         LEFT JOIN shop_member m ON m.user_id = u.id
WHERE u.del_flag = 0
ORDER BY u.create_time;
