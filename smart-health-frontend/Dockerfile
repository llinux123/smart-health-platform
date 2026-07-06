# 基于主机预构建的 dist 打包 Docker 镜像
# 在运行 docker build 前，先在主机上执行:
#   cd smart-health-frontend && npm ci && npm run build
FROM node:20-alpine AS builder
WORKDIR /build

COPY smart-health-frontend/package.json smart-health-frontend/package-lock.json ./
RUN npm ci

COPY smart-health-frontend/ .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY smart-health-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
FROM node:20-alpine AS builder
WORKDIR /build

# 1. 先复制依赖描述文件，利用 Docker layer cache
COPY smart-health-frontend/package.json smart-health-frontend/package-lock.json ./
RUN npm ci

# 2. 复制前端源码并构建
COPY smart-health-frontend/ .
RUN npm run build

# 3. Nginx 运行阶段
FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY smart-health-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
