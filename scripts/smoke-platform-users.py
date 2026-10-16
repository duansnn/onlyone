"""平台超管用户管理冒烟测试（PRD §12）。"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:41452"
PASS, FAIL = [], []
SUF = f"{int(time.time()) % 100000000:08d}"
MOBILE = "136" + SUF


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
        return status, (json.loads(raw) if raw else {})
    except json.JSONDecodeError:
        return status, {"raw": raw}


def check(name, cond, extra=""):
    (PASS if cond else FAIL).append(name)
    print(f"  [{'PASS' if cond else 'FAIL'}] {name}" + ("" if cond else f"  {extra}"))


def data(method, path, token=None, body=None):
    status, payload = call(method, path, token, body)
    assert payload.get("code") == 200, f"{method} {path} -> HTTP {status} {payload}"
    return payload["data"]


def denied(method, path, token=None, body=None):
    status, payload = call(method, path, token, body)
    assert payload.get("code") != 200, f"{method} {path} 竟然成功了"
    return payload.get("msg") or f"HTTP {status}"


print("=" * 70)
print("平台超管-用户管理")
print("=" * 70)
koko = data("POST", "/api/auth/login", body={"account": "koko", "password": "koko@2026"})
koko_token = koko["token"]

users = data("GET", "/api/platform/users?pageSize=50", koko_token)
mine = [u for u in users["rows"] if u["username"] == "koko"]
check("用户列表能查到 koko", len(mine) == 1, users["total"])
check("手机号在列表里打星", mine and "****" in mine[0]["mobileMasked"], mine)
check("koko 标识为平台管理员", mine and mine[0]["platformAdmin"] is True, mine)

# 注册一个测试用户
reg = data("POST", "/api/auth/register",
           body={"mobile": MOBILE, "password": "a123456", "nickname": "待管理用户"})
target_id = reg["user"]["id"]
check("新用户注册成功", bool(target_id), target_id)

found = data("GET", f"/api/platform/users?keyword={MOBILE}", koko_token)
check("按手机号能搜到该用户", found["total"] == 1 and found["rows"][0]["id"] == target_id, found)

# 重置密码
data("PUT", f"/api/platform/users/{target_id}/password", koko_token, {"password": "newpass123"})
call("POST", "/api/auth/login", body={"account": MOBILE, "password": "a123456"})
check("重置后旧密码不能登录",
      call("POST", "/api/auth/login", body={"account": MOBILE, "password": "a123456"})[1].get("code") != 200)
check("重置后新密码可以登录",
      call("POST", "/api/auth/login", body={"account": MOBILE, "password": "newpass123"})[1].get("code") == 200)
msg = denied("PUT", f"/api/platform/users/{target_id}/password", koko_token, {"password": "123"})
check("新密码太短被拒", "6" in msg, msg)

# 停用 / 启用
data("PUT", f"/api/platform/users/{target_id}/status?status=1", koko_token)
msg = denied("POST", "/api/auth/login", body={"account": MOBILE, "password": "newpass123"})
check("停用后不能登录", "停用" in msg, msg)
data("PUT", f"/api/platform/users/{target_id}/status?status=0", koko_token)
check("启用后可以登录",
      call("POST", "/api/auth/login", body={"account": MOBILE, "password": "newpass123"})[1].get("code") == 200)

# 不能停用自己
koko_id = koko["user"]["id"]
msg = denied("PUT", f"/api/platform/users/{koko_id}/status?status=1", koko_token)
check("不能停用自己", "自己" in msg, msg)

# 权限
owner = data("POST", "/api/auth/login", body={"account": "shopadmin", "password": "shop123"})
msg = denied("GET", "/api/platform/users", owner["token"])
check("店主访问平台用户接口被拒", "管理员" in msg or "权限" in msg, msg)
msg = denied("GET", "/api/platform/users", reg["token"])
check("普通顾客访问平台用户接口被拒", "管理员" in msg or "权限" in msg, msg)

print()
print(f"结果：PASS {len(PASS)} 项，FAIL {len(FAIL)} 项")
for name in FAIL:
    print("  - " + name)
