# -*- coding: utf-8 -*-
"""
看板后端（PRD：AI 轻量化任务看板）端到端验证

覆盖 PRD §6 全部接口 + 认证与数据隔离：
  1. 注册（重复注册拒绝）
  2. 登录（密码错误拒绝 / 成功返回 JWT）
  3. 未带 token 访问受保护接口 -> 401
  4. 项目：创建 / 列表(含任务数) / 编辑 / 删除级联
  5. 任务：新增(默认值) / 拖拽改状态 / 筛选(优先级/关键词/逾期) / 删除
  6. 统计：状态分布 + 逾期 + 趋势
  7. 导出 JSON / 导入 JSON（裸数组与备份结构两种格式）
  8. AI 接口：未配置密钥时优雅降级（600 + 明确提示）
  9. 数据隔离：用户 B 看不到用户 A 的项目
"""
import json
import sys
import time
import urllib.request
import urllib.error
import urllib.parse

BASE = "http://127.0.0.1:41999"
TIMEOUT = 30

opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

PASS = 0
FAIL = 0


def check(name, ok, detail=""):
    global PASS, FAIL
    mark = "PASS" if ok else "FAIL"
    if ok:
        PASS += 1
    else:
        FAIL += 1
    print("  [%s] %s%s" % (mark, name, ("  -> " + str(detail)) if detail else ""))


def raw(method, path, body=None, token=None, is_file=False):
    url = BASE + path
    # 防止路径中出现未编码的非 ASCII 字符（如中文标签）导致 urllib 抛 UnicodeEncodeError
    url = urllib.parse.quote(url, safe="%:/?&=+-_")
    data = None
    req_headers = {}
    if is_file:
        data = body  # 已编码的 multipart 体
        req_headers["Content-Type"] = "multipart/form-data; boundary=KBBOUNDARY"
    elif body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        req_headers["Content-Type"] = "application/json;charset=UTF-8"
    if token:
        req_headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, method=method, headers=req_headers)
    try:
        with opener.open(req, timeout=TIMEOUT) as resp:
            return resp.status, resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")


def call(method, path, body=None, token=None, is_file=False):
    status, text = raw(method, path, body, token, is_file)
    try:
        return status, json.loads(text), text
    except Exception:
        return status, None, text


def multipart_body(field, filename, content):
    b = "--KBBOUNDARY\r\n"
    b += 'Content-Disposition: form-data; name="%s"; filename="%s"\r\n' % (field, filename)
    b += "Content-Type: application/json\r\n\r\n"
    b += content + "\r\n--KBBOUNDARY--\r\n"
    return b.encode("utf-8")


def main():
    ts = str(int(time.time()))
    user_a = "e2e_a_" + ts
    user_b = "e2e_b_" + ts
    pwd = "pass123456"

    print("========== 1. 注册 ==========")
    s, js, t = call("POST", "/api/user/register", {"username": user_a, "password": pwd})
    check("用户A注册成功", s == 200 and js and js["code"] == 200, "id=%s" % (js and js.get("data")))
    check("注册返回 id 为字符串(JSON 原文带引号)", js and '"data":"' in t, t[:80])
    s, js, t = call("POST", "/api/user/register", {"username": user_a, "password": pwd})
    check("重复注册被拒绝(601)", js and js["code"] == 601, js and js.get("msg"))
    s, js, t = call("POST", "/api/user/register", {"username": "x" + ts, "password": "123"})
    check("弱密码被拒绝(400 参数校验)", js and js["code"] == 400, js and js.get("msg"))

    print("========== 2. 登录 ==========")
    s, js, t = call("POST", "/api/user/login", {"username": user_a, "password": "wrong-password"})
    check("密码错误被拒绝(600)", js and js["code"] == 600 and "用户名或密码错误" in js["msg"], js and js.get("msg"))
    s, js, t = call("POST", "/api/user/login", {"username": user_a, "password": pwd})
    check("用户A登录成功并拿到 token", s == 200 and js and js["code"] == 200 and js["data"]["token"])
    token_a = js["data"]["token"]
    check("token 为三段式 JWT", token_a.count(".") == 2, token_a[:40] + "...")
    s, js, t = call("GET", "/api/project/list")
    check("未带 token 访问项目列表 -> 401", js and js["code"] == 401, js and js.get("msg"))
    s, js, t = call("GET", "/api/project/list", token="bad.token.xxx")
    check("伪造 token -> 401", js and js["code"] == 401, js and js.get("msg"))

    # 用户 B 注册登录（用于数据隔离验证）
    call("POST", "/api/user/register", {"username": user_b, "password": pwd})
    s, js, t = call("POST", "/api/user/login", {"username": user_b, "password": pwd})
    token_b = js["data"]["token"]

    print("========== 3. 项目 ==========")
    s, js, t = call("POST", "/api/project", {"name": "作品集开发", "description": "PRD 测试项目"}, token=token_a)
    check("创建项目成功", js and js["code"] == 200 and js["data"], "id=%s" % (js and js.get("data")))
    pid = js["data"]
    s, js, t = call("GET", "/api/project/list", token=token_a)
    check("项目列表包含新项目且任务数为 0", js and any(
        p["id"] == pid and p["taskCount"] == 0 for p in js["data"]), js and js["data"])
    s, js, t = call("GET", "/api/project/list", token=token_b)
    check("数据隔离：用户B看不到用户A的项目", js and all(p["id"] != pid for p in js["data"]), js and js["data"])

    print("========== 4. 任务 CRUD 与拖拽 ==========")
    s, js, t = call("POST", "/api/task?projectId=%s" % pid,
                    {"title": "搭建前端框架"}, token=token_a)
    check("新增任务(仅标题)成功", js and js["code"] == 200 and js["data"], "id=%s" % (js and js.get("data")))
    task1 = js["data"]
    s, js, t = call("GET", "/api/task/list?projectId=%s" % pid, token=token_a)
    row = js["data"][0]
    check("缺省值：status=todo / priority=medium / overdue=false",
          row["status"] == "todo" and row["priority"] == "medium" and row["overdue"] is False,
          "status=%s priority=%s" % (row["status"], row["priority"]))

    yesterday = time.strftime("%Y-%m-%d", time.localtime(time.time() - 86400))
    s, js, t = call("POST", "/api/task?projectId=%s" % pid,
                    {"title": "编写PRD文档", "description": "含功能清单与排期",
                     "dueDate": yesterday, "priority": "high", "tags": "文档,规划"}, token=token_a)
    task2 = js["data"]
    s, js, t = call("POST", "/api/task?projectId=%s" % pid,
                    {"title": "部署上线", "priority": "low", "tags": "运维"}, token=token_a)
    task3 = js["data"]

    s, js, t = call("GET", "/api/task/list?projectId=%s&overdue=true" % pid, token=token_a)
    check("逾期筛选：仅昨天截止的任务", js and len(js["data"]) == 1 and js["data"][0]["id"] == task2,
          [x["title"] for x in js["data"]])

    # 拖拽：task1 todo -> doing -> done（只传 status）
    s, js, t = call("PUT", "/api/task/%s" % task1, {"status": "doing"}, token=token_a)
    check("拖拽改状态 todo->doing", js and js["code"] == 200)
    s, js, t = call("PUT", "/api/task/%s" % task1, {"status": "done"}, token=token_a)
    check("拖拽改状态 doing->done", js and js["code"] == 200)

    s, js, t = call("GET", "/api/task/list?projectId=%s&priority=high" % pid, token=token_a)
    check("优先级筛选 high：1 条", js and len(js["data"]) == 1, js and [x["title"] for x in js["data"]])
    s, js, t = call("GET", "/api/task/list?projectId=%s&keyword=PRD" % pid, token=token_a)
    check("关键词搜索命中『编写PRD文档』", js and len(js["data"]) == 1 and js["data"][0]["id"] == task2,
          js and [x["title"] for x in js["data"]])
    s, js, t = call("GET", "/api/task/list?projectId=%s&tag=%s" % (pid, urllib.parse.quote("文档")), token=token_a)
    check("标签筛选命中", js and len(js["data"]) == 1, js and [x["title"] for x in js["data"]])

    # 数据隔离：用户B操作用户A的任务 -> 403
    s, js, t = call("PUT", "/api/task/%s" % task1, {"status": "todo"}, token=token_b)
    check("数据隔离：用户B改用户A的任务 -> 403", js and js["code"] == 403, js and js.get("msg"))
    s, js, t = call("DELETE", "/api/project/%s" % pid, token=token_b)
    check("数据隔离：用户B删用户A的项目 -> 403", js and js["code"] == 403, js and js.get("msg"))

    print("========== 5. 统计 ==========")
    s, js, t = call("GET", "/api/stats/summary?projectId=%s" % pid, token=token_a)
    d = js["data"]
    # task1 已拖到 done；task2、task3 仍为 todo -> todo=2, doing=0, done=1, 逾期=1(task2)
    check("统计：总数3 / todo2 / doing0 / done1 / 逾期1",
          d["total"] == 3 and d["todoCount"] == 2 and d["doingCount"] == 0
          and d["doneCount"] == 1 and d["overdueCount"] == 1,
          "total=%s todo=%s doing=%s done=%s overdue=%s" % (
              d["total"], d["todoCount"], d["doingCount"], d["doneCount"], d["overdueCount"]))
    today_point = d["trend"][-1]
    check("趋势：今天新增3条、完成1条", today_point["newCount"] == 3 and today_point["doneCount"] == 1,
          today_point)
    check("趋势默认 14 天", len(d["trend"]) == 14, len(d["trend"]))

    print("========== 6. 导出 / 导入 ==========")
    s, js, t = call("GET", "/api/task/export?projectId=%s" % pid, token=token_a)
    exp = js["data"]
    check("导出：项目名/条数/任务明细齐全",
          exp["projectName"] == "作品集开发" and exp["taskCount"] == 3 and len(exp["tasks"]) == 3,
          "taskCount=%s" % exp["taskCount"])
    export_json = json.dumps(exp, ensure_ascii=False)

    # 新项目导入（备份结构）
    s, js, t = call("POST", "/api/project", {"name": "导入目标-备份结构"}, token=token_a)
    pid2 = js["data"]
    s, js, t = call("POST", "/api/task/import?projectId=%s" % pid2, token=token_a,
                    is_file=True, body=multipart_body("file", "backup.json", export_json))
    check("导入备份结构：3 条", js and js["code"] == 200 and js["data"] == 3, js and js.get("data"))

    # 裸数组导入
    s, js, t = call("GET", "/api/task/export?projectId=%s" % pid, token=token_a)
    bare = json.dumps(exp["tasks"], ensure_ascii=False)
    s, js, t = call("POST", "/api/project", {"name": "导入目标-裸数组"}, token=token_a)
    pid3 = js["data"]
    s, js, t = call("POST", "/api/task/import?projectId=%s" % pid3, token=token_a,
                    is_file=True, body=multipart_body("file", "tasks.json", bare))
    check("导入裸数组：3 条", js and js["code"] == 200 and js["data"] == 3, js and js.get("data"))

    s, js, t = call("POST", "/api/task/import?projectId=%s" % pid2, token=token_a,
                    is_file=True, body=multipart_body("file", "bad.json", "{not-valid"))
    check("非法 JSON 导入 -> 400", js and js["code"] == 400, js and js.get("msg"))

    print("========== 7. AI 接口（未配置密钥时的降级）==========")
    s, js, t = call("POST", "/api/ai/split-task", {"goal": "做一个作品集网站"}, token=token_a)
    check("AI 拆解：未配置密钥时返回 600 + 明确提示",
          js and js["code"] == 600 and "OPENAI_API_KEY" in js["msg"], js and js.get("msg"))
    s, js, t = call("POST", "/api/ai/generate-report", {"projectId": pid}, token=token_a)
    check("AI 周报：同样优雅降级", js and js["code"] == 600, js and js.get("msg"))
    s, js, t = call("POST", "/api/ai/optimize-task", {"taskId": task2}, token=token_a)
    check("AI 润色：同样优雅降级", js and js["code"] == 600, js and js.get("msg"))

    print("========== 8. 删除与级联 ==========")
    s, js, t = call("PUT", "/api/project/%s" % pid, {"name": "作品集开发(改名)", "description": "已更新"}, token=token_a)
    check("编辑项目成功", js and js["code"] == 200)
    s, js, t = call("GET", "/api/project/list", token=token_a)
    check("项目名已更新", any(p["id"] == pid and p["name"] == "作品集开发(改名)" for p in js["data"]))

    s, js, t = call("DELETE", "/api/task/%s" % task3, token=token_a)
    check("删除任务成功", js and js["code"] == 200)
    s, js, t = call("GET", "/api/task/list?projectId=%s" % pid, token=token_a)
    check("删除后列表剩 2 条", len(js["data"]) == 2, len(js["data"]))

    s, js, t = call("DELETE", "/api/project/%s" % pid, token=token_a)
    check("删除项目成功", js and js["code"] == 200)
    s, js, t = call("GET", "/api/task/list?projectId=%s" % pid, token=token_a)
    check("级联删除：任务随项目一起消失(602)", js and js["code"] == 602, js and js.get("msg"))

    print()
    print("========== 汇总：PASS=%d FAIL=%d ==========" % (PASS, FAIL))
    return 1 if FAIL else 0


if __name__ == "__main__":
    sys.exit(main())
