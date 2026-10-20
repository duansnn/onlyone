"""单商家权限矩阵冒烟：店主能管一切，顾客只能逛店面 + 管自己的订单。

覆盖：
  1. 登录返回的 role：koko=MERCHANT，新注册顾客=CUSTOMER
  2. 顾客调经营接口一律被拒（顾客列表、店铺商品、店铺看板）
  3. 店主能看顾客列表（带订单数/累计消费/地址数）、能看顾客详情（地址 + 最近订单）
  4. 店主能重置顾客密码、停用/启用；停用后顾客登不上
  5. 顾客端商品详情不下发划线价 listPrice

用法：
  python scripts/smoke-roles.py
  MALL_BASE=http://127.0.0.1:41453 python scripts/smoke-roles.py
"""
import os
import json
import time
import urllib.error
import urllib.request

BASE = os.environ.get("MALL_BASE", "http://127.0.0.1:41452")
PASS, FAIL = [], []

OWNER_ACCOUNT = os.environ.get("OWNER_ACCOUNT", "koko")
OWNER_PASSWORD = os.environ.get("OWNER_PASSWORD", "koko@2026")

STAMP = f"{int(time.time()) % 10000:04d}"
CUSTOMER_MOBILE = "135" + STAMP + "0000"
CUSTOMER_PASSWORD = "cust123456"


def call(method, path, token=None, body=None):
    headers = {"Accept": "application/json"}
    data = None
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
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
        return status, (json.loads(raw) if raw else {})
    except json.JSONDecodeError:
        return status, {"raw": raw}


def check(name, cond, extra=""):
    (PASS if cond else FAIL).append(name)
    print(f"  [{'PASS' if cond else 'FAIL'}] {name}" + ("" if cond else f"  {extra}"))


def ok(body):
    """业务成功：R.code == 200"""
    return body.get("code") == 200


def rejected(body):
    """业务被拒：R.code 是 401/403 之类"""
    return body.get("code") in (401, 403)


def main():
    print(f"BASE = {BASE}")

    # ---------- 1. 登录与角色 ----------
    print("\n[1] 登录与角色")
    st, owner = call("POST", "/api/auth/login", body={"account": OWNER_ACCOUNT, "password": OWNER_PASSWORD})
    check("店主 koko 能登录", st == 200 and ok(owner), owner)
    owner_token = (owner.get("data") or {}).get("token")
    owner_user = (owner.get("data") or {}).get("user") or {}
    owner_shops = (owner.get("data") or {}).get("shops") or []
    check("koko 角色是 MERCHANT", owner_user.get("role") == "MERCHANT", owner_user.get("role"))
    check("koko 有店铺身份", len(owner_shops) > 0, owner_shops)
    shop_id = owner_shops[0].get("shopId") if owner_shops else None

    st, reg = call("POST", "/api/auth/register",
                   body={"mobile": CUSTOMER_MOBILE, "password": CUSTOMER_PASSWORD, "nickname": "冒烟顾客"})
    check("顾客能注册", st == 200 and ok(reg), reg)
    cust_token = (reg.get("data") or {}).get("token")
    cust_user = (reg.get("data") or {}).get("user") or {}
    check("新注册账号角色是 CUSTOMER", cust_user.get("role") == "CUSTOMER", cust_user.get("role"))
    check("顾客没有店铺身份", not (reg.get("data") or {}).get("shops"), (reg.get("data") or {}).get("shops"))

    # ---------- 2. 顾客碰不到经营接口 ----------
    print("\n[2] 顾客权限边界")
    st, body = call("GET", "/api/platform/users", token=cust_token)
    check("顾客不能看顾客列表", rejected(body), f"http={st} body={body}")

    st, body = call("GET", f"/api/shop/{shop_id}/dashboard", token=cust_token)
    check("顾客不能看店铺看板", rejected(body), f"http={st} body={body}")

    st, body = call("GET", f"/api/shop/{shop_id}/products", token=cust_token)
    check("顾客不能调店铺商品接口", rejected(body), f"http={st} body={body}")

    st, body = call("GET", "/api/order/mine", token=cust_token)
    check("顾客能看自己的订单列表", st == 200 and ok(body), body)

    # ---------- 3. 店主的顾客管理 ----------
    print("\n[3] 店主的顾客管理")
    st, body = call("GET", "/api/platform/users?pageNum=1&pageSize=10", token=owner_token)
    check("店主能看顾客列表", st == 200 and ok(body), body)
    rows = ((body.get("data") or {}).get("rows") or [])
    me_row = next((r for r in rows if r.get("username") == CUSTOMER_MOBILE), None)
    check("新顾客出现在列表里", me_row is not None, [r.get("username") for r in rows])
    if me_row:
        for field in ("orderCount", "paidAmount", "addressCount", "mobile", "lastOrderTime"):
            check(f"列表行带 {field}", field in me_row, me_row)
        check("列表给的是完整手机号", me_row.get("mobile") == CUSTOMER_MOBILE, me_row.get("mobile"))
        check("店主账号不在顾客列表里", all(not r.get("merchant") for r in rows))

        st, detail = call("GET", f"/api/platform/users/{me_row['id']}", token=owner_token)
        check("店主能看顾客详情", st == 200 and ok(detail), detail)
        profile = (detail.get("data") or {}).get("profile") or {}
        check("详情里有档案", profile.get("username") == CUSTOMER_MOBILE, profile)
        check("详情里有地址数组", isinstance((detail.get("data") or {}).get("addresses"), list))
        check("详情里有订单数组", isinstance((detail.get("data") or {}).get("orders"), list))

        # ---------- 4. 停用 / 重置密码 ----------
        print("\n[4] 店主对顾客的操作")
        st, body = call("PUT", f"/api/platform/users/{me_row['id']}/password",
                        token=owner_token, body={"password": "newpass123"})
        check("店主能重置顾客密码", st == 200 and ok(body), body)
        st, body = call("POST", "/api/auth/login",
                        body={"account": CUSTOMER_MOBILE, "password": "newpass123"})
        check("顾客能用新密码登录", st == 200 and ok(body), body)

        st, body = call("PUT", f"/api/platform/users/{me_row['id']}/status?status=1", token=owner_token)
        check("店主能停用顾客", st == 200 and ok(body), body)
        st, body = call("POST", "/api/auth/login",
                        body={"account": CUSTOMER_MOBILE, "password": "newpass123"})
        check("停用后顾客登不上", not ok(body), body)
        st, body = call("PUT", f"/api/platform/users/{me_row['id']}/status?status=0", token=owner_token)
        check("店主能恢复顾客", st == 200 and ok(body), body)

    # ---------- 5. 顾客端不下发划线价 ----------
    print("\n[5] 顾客端价格")
    st, shops = call("GET", "/api/storefront/shops")
    shop = (shops.get("data") or [{}])[0] if ok(shops) else {}
    sid = shop.get("id") or shop.get("shopId")
    st, page = call("GET", f"/api/storefront/shops/{sid}/products?pageNum=1&pageSize=1")
    product = ((page.get("data") or {}).get("rows") or [{}])[0]
    pid = product.get("id")
    if pid:
        st, detail = call("GET", f"/api/storefront/shops/{sid}/products/{pid}")
        skus = (detail.get("data") or {}).get("skus") or []
        check("商品详情能取到规格", len(skus) > 0, detail)
        check("规格里没有 listPrice 字段", all("listPrice" not in s for s in skus),
              [list(s.keys()) for s in skus])
        check("规格里有售价 price", all("price" in s for s in skus))

    print(f"\n结果：PASS {len(PASS)} / FAIL {len(FAIL)}")
    if FAIL:
        print("失败项：")
        for name in FAIL:
            print("  -", name)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
