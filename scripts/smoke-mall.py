"""日用铺电商系统 —— 端到端冒烟测试（第 7、8 步：售后、评价、优惠券）。

覆盖：
  1) 店主登录、建商品上架
  2) 顾客注册、加购、下单、模拟支付
  3) 仅退款（未发货）：申请 -> 店主同意 -> 订单已退款 + 库存退回
  4) 退货退款（已发货）：申请 -> 店主同意给退货地址 -> 顾客填单号 -> 店主确认收货 -> 已退款
  5) 评价：已完成订单评价一次，重复评价被拒；店面能看到评价
  6) 优惠券：店主建券 -> 顾客领取 -> 下单抵扣 -> 支付核销
  7) 权限：顾客访问店铺后台 403；店主访问平台接口 403
"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:41452"
PASS, FAIL = [], []

# 每次运行用不同的手机号与货号，脚本可以反复跑
SUF = f"{int(time.time()) % 100000000:08d}"
CUST_MOBILE = "139" + SUF
OTHER_MOBILE = "135" + SUF
STAFF_MOBILE = "134" + SUF
SKU_CODE = "PAPER-" + SUF


def call(method, path, token=None, body=None, expect=200):
    url = BASE + path
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    # 绕开 shell 里的透明代理
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with opener.open(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            status = resp.status
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        status = e.code
    try:
        payload = json.loads(raw) if raw else {}
    except json.JSONDecodeError:
        payload = {"raw": raw}
    return status, payload


def check(name, cond, extra=""):
    if cond:
        PASS.append(name)
        print(f"  [PASS] {name}")
    else:
        FAIL.append(name)
        print(f"  [FAIL] {name}  {extra}")


def ok(method, path, token=None, body=None):
    """要求业务码 200，返回 data"""
    status, payload = call(method, path, token, body)
    if status != 200 or payload.get("code") != 200:
        raise AssertionError(f"{method} {path} 失败: HTTP {status} {json.dumps(payload, ensure_ascii=False)}")
    return payload.get("data")


def biz_fail(method, path, token=None, body=None):
    """期望业务失败，返回 msg"""
    status, payload = call(method, path, token, body)
    if status == 200 and payload.get("code") == 200:
        raise AssertionError(f"{method} {path} 竟然成功了，预期被拒")
    return payload.get("msg") or payload.get("raw") or f"HTTP {status}"


def money(fen):
    return f"¥{fen / 100:.2f}"


print("=" * 70)
print("1. 登录与建店数据")
print("=" * 70)
owner_login = ok("POST", "/api/auth/login", body={"account": "shopadmin", "password": "shop123"})
owner_token = owner_login["token"]
shop_id = owner_login["shops"][0]["shopId"]
check("店主 shopadmin 登录成功，拿到店铺", bool(shop_id), owner_login)

categories = ok("GET", f"/api/shop/{shop_id}/categories", owner_token)
flat = []
for top in categories:
    flat.extend(top.get("children") or [])
check("类目树已写入（五组两级）", len(flat) >= 15, f"二级类目 {len(flat)} 个")
leaf = flat[0]["id"]

print()
print("=" * 70)
print("2. 店主建商品并上架")
print("=" * 70)
product = ok("POST", f"/api/shop/{shop_id}/products", owner_token, {
    "categoryId": leaf,
    "name": "抽取式厨房纸巾 3 层",
    "sellingPoint": "加厚不易破，一张顶两张",
    "detail": "<p>3 层加厚，原生木浆。</p>",
    "status": "ON_SALE",
    "sort": 0,
    "images": ["https://example.com/img/paper-1.jpg"],
    "skus": [{"specName": "默认", "price": 990, "listPrice": 1290, "stock": 100,
              "skuCode": SKU_CODE, "status": "0"}],
})
product_id = product  # 雪花 ID 一律以字符串返回
detail = ok("GET", f"/api/shop/{shop_id}/products/{product_id}", owner_token)
sku_id = detail["skus"][0]["id"]
check("商品上架成功，拿到 SKU", bool(sku_id), detail)

# 上架要求校验：没有图片必须退回草稿
noimg = ok("POST", f"/api/shop/{shop_id}/products", owner_token, {
    "categoryId": leaf, "name": "无图商品测试", "status": "ON_SALE",
    "images": ["https://example.com/img/x.jpg"],
    "skus": [{"specName": "默认", "price": 100, "stock": 0, "status": "0"}],
})
noimg_detail = ok("GET", f"/api/shop/{shop_id}/products/{noimg}", owner_token)
check("没有可售 SKU 时不能直接在售（降级为草稿）",
      noimg_detail["status"] == "DRAFT", noimg_detail["status"])

print()
print("=" * 70)
print("3. 顾客注册、加购、下单、模拟支付")
print("=" * 70)
cust = ok("POST", "/api/auth/register",
          body={"mobile": CUST_MOBILE, "password": "a123456", "nickname": "小林"})
cust_token = cust["token"]
check("顾客注册即登录，身份是顾客", not cust["user"]["platformAdmin"] and len(cust["shops"]) == 0, cust)

address_id = ok("POST", "/api/address", cust_token, {
    "receiver": "小林", "mobile": CUST_MOBILE, "province": "广东省", "city": "深圳市",
    "district": "南山区", "detail": "科技园 1 号 101 室", "isDefault": True,
})
check("收货地址保存成功", bool(address_id), address_id)

ok("POST", "/api/cart", cust_token, {"skuId": sku_id, "quantity": 2})
preview = ok("POST", "/api/order/preview", cust_token,
             {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 2, "price": 990}]})
check("确认订单页计价正确（2 × 9.90）", int(preview["payAmount"]) == 1980, preview)

# 价格不一致必须拦下
price_msg = biz_fail("POST", "/api/order/submit", cust_token,
                     {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 2, "price": 1}],
                      "addressId": address_id})
check("前端价格与 SKU 不一致时停在确认页", "价格" in price_msg, price_msg)

order1 = ok("POST", "/api/order/submit", cust_token,
            {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 2, "price": 990}],
             "addressId": address_id, "buyerRemark": "发顺丰"})
order1_id = order1["orderId"]
check("下单成功，生成待付款订单", bool(order1_id), order1)

# 下单锁定了库存：可售 = 100 - 2
stock = ok("GET", f"/api/shop/{shop_id}/stock/skus?productId={product_id}", owner_token)
row = stock["rows"][0]
check("下单锁定库存（可售 98）", row["lockedStock"] == 2 and row["availableStock"] == 98, row)

pay = ok("POST", f"/api/pay/{order1_id}?channel=WECHAT", cust_token)
check("模拟支付通道已开启，可发起支付", pay["payable"] is True, pay)
mocked = ok("POST", "/api/pay/mock/callback", cust_token, {"orderId": order1_id, "channel": "WECHAT"})
check("模拟支付入账成功", mocked["accepted"] is True, mocked)

d1 = ok("GET", f"/api/order/{order1_id}", cust_token)
check("支付后订单变待发货", d1["summary"]["status"] == "PENDING_SHIPMENT", d1["summary"]["status"])

print()
print("=" * 70)
print("4. 售后（一）未发货·仅退款")
print("=" * 70)
refund1 = ok("POST", f"/api/refund/orders/{order1_id}", cust_token,
             {"type": "REFUND_ONLY", "reason": "不想要了", "description": "临时改主意了"})
refund1_detail = ok("GET", f"/api/refund/{refund1}", cust_token)
check("仅退款申请成功，状态待处理", refund1_detail["status"] == "PENDING", refund1_detail)
d1 = ok("GET", f"/api/order/{order1_id}", cust_token)
check("订单进入退款中", d1["summary"]["status"] == "REFUNDING", d1["summary"]["status"])
ship_msg = biz_fail("POST", f"/api/shop/{shop_id}/orders/{order1_id}/ship", owner_token,
                    {"shipCompany": "顺丰", "shipNo": "SF123456789"})
check("退款中的订单不能发货", "发货" in ship_msg, ship_msg)

# 顾客想看后台 -> 403
perm = biz_fail("GET", f"/api/shop/{shop_id}/orders", cust_token)
check("顾客访问店铺后台被拒", "成员" in perm or "权限" in perm, perm)

pending = ok("GET", f"/api/shop/{shop_id}/refunds?status=PENDING", owner_token)
check("后台售后台账能查到待处理", pending["total"] >= 1, pending["total"])

ok("POST", f"/api/shop/{shop_id}/refunds/{refund1}/agree", owner_token)
r1 = ok("GET", f"/api/refund/{refund1}", cust_token)
check("同意后仅退款完成（退款成功）", r1["status"] == "SUCCESS", r1)
check("退款金额 = 实付金额 19.80", int(r1["refundAmount"]) == 1980, r1["refundAmount"])
d1 = ok("GET", f"/api/order/{order1_id}", cust_token)
check("订单变已退款", d1["summary"]["status"] == "REFUNDED", d1["summary"]["status"])
stock = ok("GET", f"/api/shop/{shop_id}/stock/skus?productId={product_id}", owner_token)
row = stock["rows"][0]
check("退款后库存退回（可售 100、锁定 0）",
      row["stock"] == 100 and row["lockedStock"] == 0, row)
logs = ok("GET", f"/api/shop/{shop_id}/stock/logs?skuId={sku_id}", owner_token)
reasons = [r["reason"] for r in logs["rows"][:3]]
check("库存流水记到「售后退回」", "RETURN" in reasons, reasons)

print()
print("=" * 70)
print("5. 售后（二）已发货·退货退款，以及拒绝回到原状态")
print("=" * 70)
order2 = ok("POST", "/api/order/submit", cust_token,
            {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 1, "price": 990}],
             "addressId": address_id})
order2_id = order2["orderId"]
ok("POST", f"/api/pay/{order2_id}?channel=ALIPAY", cust_token)
ok("POST", "/api/pay/mock/callback", cust_token, {"orderId": order2_id, "channel": "ALIPAY"})
ok("POST", f"/api/shop/{shop_id}/orders/{order2_id}/ship", owner_token,
   {"shipCompany": "中通", "shipNo": "ZT998877665"})

# 已发货不能申请仅退款（此时还没有进行中的售后，能拿到状态相关的提示）
msg = biz_fail("POST", f"/api/refund/orders/{order2_id}", cust_token,
               {"type": "REFUND_ONLY", "reason": "不想要了"})
check("已发货不能申请仅退款", "退货退款" in msg, msg)

refund2 = ok("POST", f"/api/refund/orders/{order2_id}", cust_token,
             {"type": "RETURN_REFUND", "reason": "质量问题",
              "description": "拆开发现有两张破损的，想退掉"})
check("进行中时不能重复申请", "未处理完" in biz_fail(
    "POST", f"/api/refund/orders/{order2_id}", cust_token,
    {"type": "RETURN_REFUND", "reason": "质量问题"}))

ok("POST", f"/api/shop/{shop_id}/refunds/{refund2}/agree", owner_token)
r2 = ok("GET", f"/api/refund/{refund2}", cust_token)
check("同意退货退款后给出退货地址", r2["status"] == "WAITING_RETURN" and r2["returnAddress"], r2)

# 拒绝路径：另开一单验证订单回到原状态
order3 = ok("POST", "/api/order/submit", cust_token,
            {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 1, "price": 990}],
             "addressId": address_id})
order3_id = order3["orderId"]
ok("POST", f"/api/pay/{order3_id}?channel=WECHAT", cust_token)
ok("POST", "/api/pay/mock/callback", cust_token, {"orderId": order3_id, "channel": "WECHAT"})
refund3 = ok("POST", f"/api/refund/orders/{order3_id}", cust_token,
             {"type": "REFUND_ONLY", "reason": "拍错"})
msg = biz_fail("POST", f"/api/shop/{shop_id}/refunds/{refund3}/refuse", owner_token, {})
check("拒绝必须填理由", "理由" in msg or "不能为空" in msg, msg)
ok("POST", f"/api/shop/{shop_id}/refunds/{refund3}/refuse", owner_token,
   {"refuseReason": "已打包出库，无法仅退款，请改选退货退款"})
r3 = ok("GET", f"/api/refund/{refund3}", cust_token)
check("拒绝后售后单为已拒绝并带理由",
      r3["status"] == "REJECTED" and r3["refuseReason"], r3)
d3 = ok("GET", f"/api/order/{order3_id}", cust_token)
check("拒绝后订单回到待发货", d3["summary"]["status"] == "PENDING_SHIPMENT", d3["summary"]["status"])

# 顾客填退货运单号 -> 店主确认收货 -> 退款完成
ok("POST", f"/api/refund/{refund2}/return-ship", cust_token,
   {"returnCompany": "圆通", "returnShipNo": "YT1234567890"})
r2 = ok("GET", f"/api/refund/{refund2}", cust_token)
check("顾客填完退货运单号", r2["returnShipNo"] == "YT1234567890", r2)
ok("POST", f"/api/shop/{shop_id}/refunds/{refund2}/received", owner_token)
r2 = ok("GET", f"/api/refund/{refund2}", cust_token)
check("确认收货后退款完成", r2["status"] == "SUCCESS", r2)
d2 = ok("GET", f"/api/order/{order2_id}", cust_token)
check("退货退款订单变已退款", d2["summary"]["status"] == "REFUNDED", d2["summary"]["status"])
stock = ok("GET", f"/api/shop/{shop_id}/stock/skus?productId={product_id}", owner_token)
row = stock["rows"][0]
check("退货后退回库存（100 - 1(order3 在途中未退) = 99）", row["stock"] == 99, row)

print()
print("=" * 70)
print("6. 评价")
print("=" * 70)
order4 = ok("POST", "/api/order/submit", cust_token,
            {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 1, "price": 990}],
             "addressId": address_id})
order4_id = order4["orderId"]
ok("POST", f"/api/pay/{order4_id}?channel=WECHAT", cust_token)
ok("POST", "/api/pay/mock/callback", cust_token, {"orderId": order4_id, "channel": "WECHAT"})
msg = biz_fail("POST", "/api/review", cust_token,
               {"orderId": order4_id, "score": 5, "content": "还没收货就想评价一下试试"})
check("未完成不能评价", "已完成" in msg, msg)

ok("POST", f"/api/shop/{shop_id}/orders/{order4_id}/ship", owner_token,
   {"shipCompany": "韵达", "shipNo": "YD556677889"})
ok("POST", f"/api/order/{order4_id}/confirm", cust_token)
msg = biz_fail("POST", "/api/review", cust_token,
               {"orderId": order4_id, "score": 5, "content": "字数不够"})
check("评价文字不足 10 字被拒", "10" in msg, msg)

review_id = ok("POST", "/api/review", cust_token,
               {"orderId": order4_id, "score": 5,
                "content": "纸巾很厚实，沾水也不容易破，回购了。",
                "imageUrls": ["https://example.com/img/r1.jpg"]})
check("评价提交成功", bool(review_id), review_id)
msg = biz_fail("POST", "/api/review", cust_token,
               {"orderId": order4_id, "score": 1, "content": "想改成差评，应该改不了才对"})
check("一笔订单只能评一次，提交后不能改", "评价过" in msg, msg)

d4 = ok("GET", f"/api/order/{order4_id}", cust_token)
check("订单已标记为已评价（canReview=false）", d4["summary"]["canReview"] is False, d4["summary"])

reviews = ok("GET", f"/api/storefront/shops/{shop_id}/products/{product_id}/reviews?pageSize=10")
check("店面能看到该商品的评价", reviews["total"] >= 1 and reviews["rows"][0]["score"] == 5, reviews)
check("评价带上评价人昵称", reviews["rows"][0]["nickname"] == "小林", reviews["rows"][0])

shop_reviews = ok("GET", f"/api/shop/{shop_id}/reviews", owner_token)
check("后台评价列表只读可看", shop_reviews["total"] >= 1, shop_reviews["total"])

print()
print("=" * 70)
print("7. 优惠券接到结算")
print("=" * 70)
coupon_id = ok("POST", f"/api/shop/{shop_id}/coupons", owner_token, {
    "name": "满 20 减 3", "thresholdAmount": 2000, "reduceAmount": 300,
    "total": 50, "perUserLimit": 1, "status": "0",
})
check("店主建券成功", bool(coupon_id), coupon_id)
msg = biz_fail("POST", f"/api/shop/{shop_id}/coupons", owner_token, {
    "name": "减免大于门槛", "thresholdAmount": 100, "reduceAmount": 500,
    "total": 10, "perUserLimit": 1, "status": "0"})
check("减免金额必须小于门槛", "门槛" in msg, msg)

center = ok("GET", f"/api/coupon/center?shopId={shop_id}&pageSize=10", cust_token)
check("领券中心能看到刚建的券", any(c["id"] == coupon_id for c in center["rows"]), center["rows"])

coupon_user_id = ok("POST", f"/api/coupon/{coupon_id}/receive", cust_token)
check("顾客领券成功", bool(coupon_user_id), coupon_user_id)
msg = biz_fail("POST", f"/api/coupon/{coupon_id}/receive", cust_token)
check("超过每人限领被拒", "限领" in msg, msg)

# 门槛不足时不可用：1 件 9.90 < 20.00
usable = ok("GET", f"/api/coupon/usable?shopId={shop_id}&goodsAmount=990", cust_token)
check("未达门槛时不出现可用券", all(c["id"] != coupon_user_id for c in usable), usable)

order5 = ok("POST", "/api/order/submit", cust_token,
            {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 3, "price": 990}],
             "addressId": address_id, "couponUserId": coupon_user_id})
order5_detail = ok("GET", f"/api/order/{order5['orderId']}", cust_token)
o5 = order5_detail["summary"]
check("满 29.70 减 3.00，应付 26.70", int(o5["payAmount"]) == 2670 and int(o5["discountAmount"]) == 300, o5)
mine = ok("GET", f"/api/coupon/mine?shopId={shop_id}", cust_token)
used = [c for c in mine if c["id"] == coupon_user_id]
check("下单后券被占用（LOCKED）", used and used[0]["status"] == "LOCKED", used)

ok("POST", f"/api/pay/{order5['orderId']}?channel=WECHAT", cust_token)
ok("POST", "/api/pay/mock/callback", cust_token,
   {"orderId": order5["orderId"], "channel": "WECHAT"})
mine = ok("GET", f"/api/coupon/mine?shopId={shop_id}", cust_token)
used = [c for c in mine if c["id"] == coupon_user_id]
check("支付成功后券被核销（USED）", used and used[0]["status"] == "USED", used)

# 取消待付款订单 -> 券退回
coupon_id2 = ok("POST", f"/api/shop/{shop_id}/coupons", owner_token, {
    "name": "满 10 减 1", "thresholdAmount": 1000, "reduceAmount": 100,
    "total": 50, "perUserLimit": 2, "status": "0"})
cu2 = ok("POST", f"/api/coupon/{coupon_id2}/receive", cust_token)
order6 = ok("POST", "/api/order/submit", cust_token,
            {"shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 2, "price": 990}],
             "addressId": address_id, "couponUserId": cu2})
ok("POST", f"/api/order/{order6['orderId']}/cancel", cust_token, {"reason": "信息填错"})
mine = ok("GET", f"/api/coupon/mine?shopId={shop_id}", cust_token)
cu2_row = [c for c in mine if c["id"] == cu2]
check("取消订单后券退回（RETURNED）", cu2_row and cu2_row[0]["status"] == "RETURNED", cu2_row)

print()
print("=" * 70)
print("8. 权限核对")
print("=" * 70)
koko = ok("POST", "/api/auth/login", body={"account": "koko", "password": "koko@2026"})
koko_token = koko["token"]
check("平台超管 koko 登录成功", koko["user"]["platformAdmin"] is True, koko["user"])
overview = ok("GET", "/api/platform/shops/overview", koko_token)
check("平台工作台能看汇总数字", overview["shopCount"] >= 1, overview)
msg = biz_fail("GET", f"/api/shop/{shop_id}/orders", koko_token)
check("koko 不是店员，访问店铺后台被拒", "成员" in msg, msg)
msg = biz_fail("GET", "/api/platform/shops/overview", owner_token)
check("店主访问平台接口被拒", "权限" in msg or "管理员" in msg, msg)
msg = biz_fail("POST", f"/api/shop/{shop_id}/coupons", cust_token, {
    "name": "顾客想建券", "thresholdAmount": 1000, "reduceAmount": 100, "total": 1, "status": "0"})
check("顾客建券被拒", "成员" in msg, msg)

# 店员不能建券
ok("POST", f"/api/shop/{shop_id}/members", owner_token,
   {"mobile": STAFF_MOBILE, "password": "staff123", "nickname": "店员小周", "role": "STAFF"})
staff = ok("POST", "/api/auth/login", body={"account": STAFF_MOBILE, "password": "staff123"})
staff_token = staff["token"]
msg = biz_fail("POST", f"/api/shop/{shop_id}/coupons", staff_token, {
    "name": "店员想建券", "thresholdAmount": 1000, "reduceAmount": 100, "total": 1, "status": "0"})
check("店员建券被拒（仅店主可做）", "店主" in msg, msg)
ok("POST", f"/api/shop/{shop_id}/stock/adjust", staff_token,
   {"skuId": sku_id, "stock": 120, "reason": "MANUAL", "remark": "盘点补货"})
check("店员可以调库存", True)
msg = biz_fail("PUT", f"/api/shop/{shop_id}/profile", staff_token,
               {"name": "店员改店名", "version": 0})
check("店员改店铺资料被拒", "店主" in msg, msg)

# 用别人的订单号访问
other = ok("POST", "/api/auth/register",
           body={"mobile": OTHER_MOBILE, "password": "a123456", "nickname": "路人"})
msg = biz_fail("GET", f"/api/order/{order4_id}", other["token"])
check("用别人的订单号访问返回不存在", "不存在" in msg, msg)
msg = biz_fail("GET", f"/api/refund/{refund2}", other["token"])
check("用别人的售后编号访问返回不存在", "不存在" in msg, msg)

print()
print("=" * 70)
print(f"结果：PASS {len(PASS)} 项，FAIL {len(FAIL)} 项")
if FAIL:
    print("失败项：")
    for name in FAIL:
        print("  - " + name)
print("=" * 70)
