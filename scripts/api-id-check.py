# -*- coding: utf-8 -*-
"""
雪花 ID 精度修复验证（API 层）

校验点：
1. 创建用户返回的 id 在原始 JSON 中是【带引号的字符串】，而不是裸数字
   —— 裸数字会在浏览器 JSON.parse 时被 Number 截断（Number.MAX_SAFE_INTEGER = 9007199254740991，19 位雪花 ID 必然溢出）
2. 用返回的 id 原样回传做 PUT 更新、DELETE 删除，均成功（证明前后端 ID 往返无损）
3. 列表接口里的 id 同样是字符串
"""
import json
import sys
import time
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:41452"
TIMEOUT = 15

opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def raw(method, path, body=None):
    url = BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json;charset=UTF-8")
    try:
        with opener.open(req, timeout=TIMEOUT) as resp:
            return resp.status, resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")


def call(method, path, body=None):
    status, text = raw(method, path, body)
    try:
        return status, json.loads(text), text
    except Exception:
        return status, None, text


results = []


def check(name, ok, detail=""):
    results.append((name, ok, detail))
    print(("PASS  " if ok else "FAIL  ") + name + ("  :: " + str(detail) if detail else ""))


# ---------- 0. 健康检查 ----------
status, js, _ = call("GET", "/api/portal/info")
check("GET /api/portal/info 可访问", status == 200 and js and js.get("code") == 200,
      "status=%s code=%s" % (status, js.get("code") if js else None))

# ---------- 1. 创建用户：id 必须是字符串 ----------
uname = "idcheck_%d" % int(time.time())
status, js, text = call("POST", "/api/portal/users", {
    "username": uname,
    "nickname": "精度校验用户",
    "password": "Test@123456",
    "email": "idcheck@owner.dev",
    "mobile": "13900001111",
    "status": 1,
})
ok = status == 200 and js and js.get("code") == 200
check("POST /api/portal/users 创建成功", ok, "status=%s code=%s msg=%s" % (status, js.get("code") if js else None, js.get("msg") if js else text[:120]))
if not ok:
    print("\n无法继续：创建用户失败")
    sys.exit(1)

new_id = js.get("data")
check("创建接口 data 返回 id", bool(new_id), "id=%r" % (new_id,))
is_str = isinstance(new_id, str)
check("id 为字符串类型（精度保护）", is_str, "type=%s" % type(new_id).__name__)

# 原始 JSON 里必须是 "data":"2110..." 带引号形式
quoted = ('"data":"%s"' % new_id) in text.replace(" ", "") if is_str else False
check("原始 JSON 中 id 带引号（非裸数字）", quoted, text[:160])

if is_str and len(new_id) >= 19:
    check("雪花 id 为 19 位", True, "len=%d id=%s" % (len(new_id), new_id))

# ---------- 2. 详情查询 ----------
status, js, text = call("GET", "/api/portal/users/" + str(new_id))
check("GET /api/portal/users/{id} 详情成功", status == 200 and js and js.get("code") == 200 and js.get("data"),
      "code=%s" % (js.get("code") if js else None))
detail = js.get("data") or {}
check("详情 id 与创建 id 完全一致", detail.get("id") == new_id,
      "created=%s detail=%s" % (new_id, detail.get("id")))
check("详情含 version 字段（乐观锁）", "version" in detail, "version=%s" % detail.get("version"))
check("手机号已脱敏", "****" in str(detail.get("mobile", "")), "mobile=%s" % detail.get("mobile"))

# ---------- 3. 更新（乐观锁 + id 无损往返） ----------
ver = detail.get("version", 0)
status, js, text = call("PUT", "/api/portal/users", {
    "id": new_id,
    "nickname": "精度校验用户-已改名",
    "email": "idcheck2@owner.dev",
    "mobile": "13900002222",
    "status": 1,
    "version": ver,
})
ok = status == 200 and js and js.get("code") == 200
check("PUT 更新成功（id 往返无损）", ok, "code=%s msg=%s" % (js.get("code") if js else None, js.get("msg") if js else text[:120]))

status, js, _ = call("GET", "/api/portal/users/" + str(new_id))
check("更新后昵称已生效", (js.get("data") or {}).get("nickname") == "精度校验用户-已改名",
      "nickname=%s" % (js.get("data") or {}).get("nickname"))

# ---------- 4. 列表接口 id 也是字符串 ----------
status, js, text = call("GET", "/api/portal/users?pageNum=1&pageSize=10")
page = (js.get("data") or {}) if js else {}
recs = page.get("rows") or page.get("records") or []
ids_ok = bool(recs) and all(isinstance(r.get("id"), str) for r in recs)
check("列表 rows[].id 全部为字符串", ids_ok,
      "count=%d sample=%s" % (len(recs), recs[0].get("id") if recs else None))
check("分页 total 保持数值类型", isinstance(page.get("total"), int),
      "total=%s type=%s" % (page.get("total"), type(page.get("total")).__name__))

# ---------- 5. 逻辑删除 ----------
status, js, text = call("DELETE", "/api/portal/users/" + str(new_id))
ok = status == 200 and js and js.get("code") == 200
check("DELETE 删除成功（id 往返无损）", ok, "code=%s msg=%s" % (js.get("code") if js else None, js.get("msg") if js else text[:120]))

status, js, _ = call("GET", "/api/portal/users/" + str(new_id))
gone = status == 200 and js and js.get("code") != 200
check("删除后详情已不可见（逻辑删除生效）", gone, "code=%s msg=%s" % (js.get("code") if js else None, js.get("msg") if js else None))

print("\n================ 汇总 ================")
passed = sum(1 for _, ok, _ in results if ok)
print("API 层：%d/%d 通过" % (passed, len(results)))
sys.exit(0 if passed == len(results) else 2)
