# ======================================================================
#  Owner IntelliPortal 前端镜像
#  构建：node:22-alpine + Vite   →   运行：nginx:1.27-alpine
# ======================================================================

# ---------------------------- 构建阶段 ----------------------------
FROM node:22-alpine AS builder

WORKDIR /build

COPY package.json package-lock.json* ./
# 有 lock 文件时用 npm ci 保证可复现；否则退回 npm install
RUN if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; \
    else npm install --no-audit --no-fund; fi

COPY . .
RUN npm run build

# ---------------------------- 运行阶段 ----------------------------
FROM nginx:1.27-alpine

ENV TZ=Asia/Shanghai
RUN ln -snf "/usr/share/zoneinfo/${TZ}" /etc/localtime && echo "${TZ}" > /etc/timezone

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
