"""库存链路 + 商品删除 + 评价删除 的一次性验收脚本（可反复跑）。

覆盖：
  1) 下单只锁库存，不动总库存；确认收款才扣减；退款完成回补
  2) 有未完结订单的商品删不掉；订单取消后可以删
  3) 店主能删评价，店员不能

用法：python scripts/smoke-stock-chain.py
环境变量 MALL_BASE 可覆盖后端地址（默认 http://127.0.0.1:41452）
"""
import json
import os
import time
import urllib.error
import urllib.request
from urllib.parse import quote

BASE = os.environ.get("MALL_BASE", "http://127.0.0.1:41452")
PASS, FAIL = [], []

SUF = f"{int(time.time()) % 100000000:08d}"
CUST_MOBILE = "137" + SUF
# 店员固定用同一个号，避免每次运行都往店员列表里塞一个
STAFF_MOBILE = "13500000001"


def call(method, path, token=None, body=None):
    headers = {"Accept": "application/json"}
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with opener.open(req, timeout=30) as resp:
            raw, status = resp.read().decode("utf-8"), resp.status
    except urllib.error.HTTPError as e:
        raw, status = e.read().decode("utf-8"), e.code
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
    status, payload = call(method, path, token, body)
    if status != 200 or payload.get("code") != 200:
        raise AssertionError(
            f"{method} {path} 失败: HTTP {status} {json.dumps(payload, ensure_ascii=False)}")
    return payload.get("data")


def biz_fail(method, path, token=None, body=None):
    status, payload = call(method, path, token, body)
    if status == 200 and payload.get("code") == 200:
        raise AssertionError(f"{method} {path} 竟然成功了，预期被拒")
    return payload.get("msg") or payload.get("raw") or f"HTTP {status}"


def stock_of(shop_id, token, product_id):
    page = ok("GET", f"/api/shop/{shop_id}/stock/skus?productId={product_id}&pageSize=10", token)
    row = page["rows"][0]
    return row["skuId"], row


print("=" * 70)
print("0. 准备：店主登录、类目、顾客注册")
print("=" * 70)
owner = ok("POST", "/api/auth/login", body={"account": "koko", "password": "koko@2026"})
owner_token = owner["token"]
shop_id = owner["shops"][0]["shopId"]
check("店主 koko 登录成功", bool(shop_id), owner)

categories = ok("GET", f"/api/shop/{shop_id}/categories", owner_token)
leaf = next(c["id"] for top in categories for c in (top.get("children") or []))

cust = ok("POST", "/api/auth/register",
          body={"mobile": CUST_MOBILE, "password": "a123456", "nickname": "库存验证客"})
cust_token = cust["token"]
address_id = ok("POST", "/api/address", cust_token, {
    "receiver": "库存验证客", "mobile": CUST_MOBILE, "province": "广东省", "city": "深圳市",
    "district": "南山区", "detail": "科技园 1 号 101 室", "isDefault": True,
})
check("顾客注册并建好收货地址", bool(address_id))

print()
print("=" * 70)
print("1. 库存链路：下单锁定 → 确认收款扣减 → 退款回补")
print("=" * 70)
product_id = ok("POST", f"/api/shop/{shop_id}/products", owner_token, {
    "categoryId": leaf, "name": "库存链路验证商品", "sellingPoint": "库存从 10 开始",
    "status": "ON_SALE", "sort": 0, "images": ["/demo/p3.jpg"],
    "skus": [{"specName": "默认", "price": 1000, "stock": 10, "status": "0"}],
})
sku_id, row = stock_of(shop_id, owner_token, product_id)
print(f"  建品后：总库存 {row['stock']}，锁定 {row['lockedStock']}，可售 {row['availableStock']}")
check("建品后总库存 = 10", row["stock"] == 10 and row["availableStock"] == 10, row)

order = ok("POST", "/api/order/submit", cust_token, {
    "shopId": shop_id, "items": [{"skuId": sku_id, "quantity": 2, "price": 1000}],
    "addressId": address_id,
})
order_id = order["orderId"]
_, row = stock_of(shop_id, owner_token, product_id)
print(f"  下单后：总库存 {row['stock']}，锁定 {row['lockedStock']}，可售 {row['availableStock']}")
check("下单只锁定，不改总库存（10 / 锁 2 / 可售 8）",
      row["stock"] == 10 and row["lockedStock"] == 2 and row["availableStock"] == 8, row)

ok("POST", f"/api/shop/{shop_id}/orders/{order_id}/confirm-payment", owner_token)
_, row = stock_of(shop_id, owner_token, product_id)
print(f"  确认收款后：总库存 {row['stock']}，锁定 {row['lockedStock']}，可售 {row['availableStock']}")
check("确认收款后总库存 10 → 8，锁定归零",
      row["stock"] == 8 and row["lockedStock"] == 0, row)

refund_id = ok("POST", f"/api/refund/orders/{order_id}", cust_token,
               {"type": "REFUND_ONLY", "reason": "不想要了", "description": "验证库存回补"})
ok("POST", f"/api/shop/{shop_id}/refunds/{refund_id}/agree", owner_token)
_, row = stock_of(shop_id, owner_token, product_id)
print(f"  退款完成后：总库存 {row['stock']}，锁定 {row['lockedStock']}，可售 {row['availableStock']}")
check("退款完成库存回补 8 → 10", row["stock"] == 10, row)

logs = ok("GET", f"/api/shop/{shop_id}/stock/logs?skuId={sku_id}&pageSize=20", owner_token)
reasons = {item["reason"] for item in logs["rows"]}
print(f"  库存流水原因：{sorted(reasons)}")
check("流水里能看到 下单锁定(LOCK) / 支付扣减(PAY) / 售后退回(RETURN)",
      {"LOCK", "PAY", "RETURN"} <= reasons, reasons)

print()
print("=" * 70)
print("2. 商品删除：有未完结订单时拒绝，订单结束后放行")
print("=" * 70)
product2 = ok("POST", f"/api/shop/{shop_id}/products", owner_token, {
    "categoryId": leaf, "name": "待删除验证商品", "status": "ON_SALE",
    "images": ["/demo/p4.jpg"],
    "skus": [{"specName": "默认", "price": 500, "stock": 5, "status": "0"}],
})
sku2, _ = stock_of(shop_id, owner_token, product2)
order2 = ok("POST", "/api/order/submit", cust_token, {
    "shopId": shop_id, "items": [{"skuId": sku2, "quantity": 1, "price": 500}],
    "addressId": address_id,
})
msg = biz_fail("DELETE", f"/api/shop/{shop_id}/products/{product2}", owner_token)
check("有未完结订单时删除被拒", "未完结" in msg, msg)

ok("POST", f"/api/order/{order2['orderId']}/cancel", cust_token, {"reason": "先不买了"})
ok("DELETE", f"/api/shop/{shop_id}/products/{product2}", owner_token)
page = ok("GET", f"/api/shop/{shop_id}/products?keyword={quote('待删除验证商品')}", owner_token)
check("订单取消后删除成功，列表里查不到", page["total"] == 0, page)

print()
print("=" * 70)
print("3. 评价删除：店主可删，店员被拒")
print("=" * 70)
# 再造一单走到「已完成」
product3 = ok("POST", f"/api/shop/{shop_id}/products", owner_token, {
    "categoryId": leaf, "name": "评价删除验证商品", "status": "ON_SALE",
    "images": ["/demo/p5.jpg"],
    "skus": [{"specName": "默认", "price": 800, "stock": 5, "status": "0"}],
})
sku3, _ = stock_of(shop_id, owner_token, product3)
order3 = ok("POST", "/api/order/submit", cust_token, {
    "shopId": shop_id, "items": [{"skuId": sku3, "quantity": 1, "price": 800}],
    "addressId": address_id,
})
ok("POST", f"/api/shop/{shop_id}/orders/{order3['orderId']}/confirm-payment", owner_token)
ok("POST", f"/api/shop/{shop_id}/orders/{order3['orderId']}/ship", owner_token,
   {"shipCompany": "顺丰", "shipNo": "SF" + SUF})
ok("POST", f"/api/order/{order3['orderId']}/confirm", cust_token)
detail = ok("GET", f"/api/order/{order3['orderId']}", cust_token)
check("订单走到已完成", detail["summary"]["status"] == "FINISHED", detail["summary"]["status"])

review_id = ok("POST", "/api/review", cust_token,
               {"orderId": order3["orderId"], "score": 4, "content": "东西不错，就是包装有点简单"})
check("顾客提交评价成功", bool(review_id), review_id)

# 店员账号：店主代建（不需要店员自己注册）。已建过就跳过，不重复加人
try:
    ok("POST", f"/api/shop/{shop_id}/members", owner_token, {
        "mobile": STAFF_MOBILE, "password": "staff123", "nickname": "库存验证店员", "role": "STAFF",
    })
except AssertionError:
    print("  (店员账号已存在，复用)")
staff = ok("POST", "/api/auth/login", body={"account": STAFF_MOBILE, "password": "staff123"})
staff_token = staff["token"]
check("店主代建店员账号，店员能直接登录后台", bool(staff["shops"]), staff)

msg = biz_fail("DELETE", f"/api/shop/{shop_id}/reviews/{review_id}", staff_token)
check("店员删评价被拒", "店主" in msg or "权限" in msg, msg)

ok("DELETE", f"/api/shop/{shop_id}/reviews/{review_id}", owner_token)
rows = ok("GET", f"/api/shop/{shop_id}/reviews?pageSize=50", owner_token)["rows"]
check("店主删除评价成功，列表里不再出现", all(str(r["id"]) != str(review_id) for r in rows))

# 收尾：把本轮造的商品都软删掉，不留垃圾
for pid in (product_id, product3):
    try:
        ok("DELETE", f"/api/shop/{shop_id}/products/{pid}", owner_token)
    except AssertionError as e:
        print(f"  [WARN] 清理商品 {pid} 失败：{e}")

print()
print("=" * 70)
print(f"结果：PASS {len(PASS)} 项，FAIL {len(FAIL)} 项")
print("=" * 70)
if FAIL:
    raise SystemExit(1)
