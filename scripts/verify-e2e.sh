#!/bin/bash
# 端到端联调验证：启动后端 -> API 精度校验 -> 前端构建+preview -> Playwright UI 冒烟
# 一次性在同一个 shell 内跑完（后台进程不跨 turn 存活）

set -u

export JAVA_HOME="D:/soft/jdk25/jdk-25_windows-x64_bin/jdk-25.0.4"
JAVA_BIN="/d/soft/jdk25/jdk-25_windows-x64_bin/jdk-25.0.4/bin/java.exe"
# 注意：Git Bash 不认 "D:/..." 形式的 PATH 项，必须用绝对路径直接调 java.exe，
# 否则会命中 PATH 里 Oracle 的 javapath 软链（Java 17），报 UnsupportedClassVersionError
export PATH="$JAVA_HOME/bin:$PATH"
# 关键：绕开本机透明代理，否则对 localhost 的请求会被转发到 127.0.0.1:64304 并返回 502
export no_proxy="localhost,127.0.0.1,::1"
export NO_PROXY="localhost,127.0.0.1,::1"
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY
# 环境里预设了 SERVER__PORT=64271 / SERVER__HOST，Spring Boot 宽松绑定会把它映射到
# server.port / server.address，必须清掉，端口以命令行 --server.port=41452 为准
unset SERVER__PORT SERVER__HOST SERVER_PORT SERVER_ADDRESS

PROJ=/d/code/onlyone
LOGS=$PROJ/logs
mkdir -p "$LOGS"
BACKEND_LOG=$LOGS/backend.log
PREVIEW_LOG=$LOGS/preview.log
: > "$BACKEND_LOG"
: > "$PREVIEW_LOG"

BACKEND_PID=""
PREVIEW_PID=""

cleanup() {
  echo ""
  echo "=========== 清理 ==========="
  [ -n "$PREVIEW_PID" ] && kill "$PREVIEW_PID" 2>/dev/null && echo "已停止 preview (pid=$PREVIEW_PID)"
  [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null && echo "已停止 backend (pid=$BACKEND_PID)"
  sleep 1
}
trap cleanup EXIT

cd "$PROJ" || exit 1

echo "=========== 0. 释放残留端口 ==========="
for port in 41452 4173; do
  pid=$(netstat -ano 2>/dev/null | grep -E "LISTENING" | grep -E ":$port " | awk '{print $5}' | head -1)
  if [ -n "$pid" ]; then
    echo "端口 $port 被 pid=$pid 占用，正在释放"
    taskkill //F //PID "$pid" >/dev/null 2>&1 && echo "  已终止 $pid"
  else
    echo "端口 $port 空闲"
  fi
done
sleep 2

echo ""
echo "=========== 1. 启动后端 (端口 41452) ==========="
echo "使用 JDK: $("$JAVA_BIN" -version 2>&1 | head -1)"
"$JAVA_BIN" -version >/dev/null 2>&1 || { echo "找不到 JDK 25: $JAVA_BIN"; exit 1; }
# 与运行镜像保持一致：-Xms512m -Xmx512m + Asia/Shanghai
"$JAVA_BIN" -Xms512m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
  -jar owner-server/target/owner-server.jar \
  --spring.profiles.active=local --server.port=41452 >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
echo "backend pid=$BACKEND_PID，等待就绪..."

READY=0
for i in $(seq 1 120); do
  code=$(curl -s -o /dev/null -w '%{http_code}' --noproxy '*' http://127.0.0.1:41452/api/portal/info 2>/dev/null)
  if [ "$code" = "200" ]; then
    READY=1
    echo "后端已就绪（等待 ${i}s）"
    break
  fi
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "后端进程已退出，日志尾部："
    tail -40 "$BACKEND_LOG"
    exit 1
  fi
  sleep 1
done

if [ "$READY" != "1" ]; then
  echo "后端 120s 内未就绪，日志尾部："
  tail -50 "$BACKEND_LOG"
  exit 1
fi

echo "启动日志关键行："
grep -E "Started OwnerApplication|Tomcat started|The following .* profiles|port" "$BACKEND_LOG" | tail -5

echo ""
echo "=========== 2. 雪花 ID 精度修复验证（API 层）==========="
PY="C:/Users/17535/.workbuddy/binaries/python/versions/3.13.12/python.exe"
"$PY" "$PROJ/scripts/api-id-check.py"
API_RC=$?
echo "API 校验退出码：$API_RC"

echo ""
echo "=========== 3. 前端类型检查 + 生产构建 ==========="
cd "$PROJ/owner-web" || exit 1
export NODE_OPTIONS=""
npm run build 2>&1 | tail -20
BUILD_RC=${PIPESTATUS[0]}
echo "前端构建退出码：$BUILD_RC"

echo ""
echo "=========== 4. 启动前端 preview (4173) ==========="
npm run preview >"$PREVIEW_LOG" 2>&1 &
PREVIEW_PID=$!
PR_READY=0
for i in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w '%{http_code}' --noproxy '*' http://127.0.0.1:4173/ 2>/dev/null)
  if [ "$code" = "200" ]; then
    PR_READY=1
    echo "preview 已就绪（等待 ${i}s）"
    break
  fi
  sleep 1
done
[ "$PR_READY" = "1" ] || { echo "preview 未就绪"; tail -20 "$PREVIEW_LOG"; }

echo ""
echo "=========== 5. Playwright UI 冒烟测试 ==========="
cd /c/Users/17535/.workbuddy/binaries/node/workspace || exit 1
NODE_PATH="C:/Users/17535/.workbuddy/binaries/node/workspace/node_modules" \
  "/c/Users/17535/.workbuddy/binaries/node/versions/22.22.2-6/node.exe" ui-smoke.mjs
SMOKE_RC=$?
echo "UI 冒烟退出码：$SMOKE_RC"

echo ""
echo "=========== 最终汇总 ==========="
echo "API 校验   : $([ $API_RC -eq 0 ] && echo '全部通过' || echo "有失败 (rc=$API_RC)")"
echo "前端构建   : $([ $BUILD_RC -eq 0 ] && echo '成功' || echo "失败 (rc=$BUILD_RC)")"
echo "UI 冒烟    : $([ $SMOKE_RC -eq 0 ] && echo '全部通过' || echo "有失败 (rc=$SMOKE_RC)")"
echo "后端日志   : $BACKEND_LOG"
echo "前端日志   : $PREVIEW_LOG"
exit 0
