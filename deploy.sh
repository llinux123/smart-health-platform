#!/bin/bash
set -euo pipefail

# ============================================================
# Smart Health Platform - Docker Deployment Script
# 用法:
#   ./deploy.sh build          # 本地构建所有 Docker 镜像
#   ./deploy.sh up             # 本地启动所有服务
#   ./deploy.sh down           # 停止所有服务
#   ./deploy.sh restart        # 重启所有服务
#   ./deploy.sh logs [service] # 查看日志
#   ./deploy.sh clean          # 清理数据卷（⚠️ 会删除数据库数据）
#   ./deploy.sh deploy <user@host> [ssh-port]  # 打包部署到远程虚拟机
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPT_NAME="$(basename "$0")"
COMPOSE_FILE="docker-compose.yml"
DEPLOY_BUNDLE="/tmp/smart-health-deploy.tar.gz"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查依赖
check_prereqs() {
    if ! command -v docker &>/dev/null; then
        error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! docker compose version &>/dev/null && ! docker-compose --version &>/dev/null; then
        error "Docker Compose 未安装"
        exit 1
    fi

    # 需要 Docker BuildKit
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
}

# ===== 构建 =====
cmd_build() {
    info "开始构建所有 Docker 镜像..."
    cd "$PROJECT_DIR"

    info "1. 构建业务服务 (smart-health-app)..."
    docker build -f smart-health-app/Dockerfile -t smart-health-app:latest .

    info "2. 构建 API 网关 (smart-gateway)..."
    docker build -f smart-gateway/Dockerfile -t smart-health-gateway:latest .

    info "3. 构建前端 (smart-health-frontend)..."
    docker build -f smart-health-frontend/Dockerfile -t smart-health-frontend:latest .

    info "✅ 所有镜像构建完成"
    docker images --filter "reference=smart-health-*" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
}

# ===== 启动 =====
cmd_up() {
    info "启动所有服务..."
    cd "$PROJECT_DIR"

    if [ ! -f .env ]; then
        warn ".env 文件不存在，请创建并配置 AI_API_KEY 和 ZHIPU_API_KEY"
        warn "参见 .env.example"
    fi

    # --build 确保源码变更后自动重新构建镜像
    docker compose -f "$COMPOSE_FILE" up -d --build

    info "✅ 所有服务已启动"
    echo ""
    echo "服务访问地址："
    echo "  前端页面:      http://localhost"
    echo "  API 网关:      http://localhost:9000"
    echo "  后端服务:      http://localhost:8080"
    echo "  Nacos 控制台:  http://localhost:8848/nacos"
    echo "  RabbitMQ 管理: http://localhost:15672 (guest/guest)"
    echo ""
    echo "查看启动日志: $SCRIPT_NAME logs"
}

# ===== 停止 =====
cmd_down() {
    info "停止所有服务..."
    cd "$PROJECT_DIR"
    docker compose -f "$COMPOSE_FILE" down
    info "✅ 所有服务已停止"
}

# ===== 重启 =====
cmd_restart() {
    cmd_down
    cmd_up
}

# ===== 查看日志 =====
cmd_logs() {
    cd "$PROJECT_DIR"
    if [ $# -gt 0 ]; then
        docker compose -f "$COMPOSE_FILE" logs -f "$1"
    else
        docker compose -f "$COMPOSE_FILE" logs -f
    fi
}

# ===== 清理 =====
cmd_clean() {
    warn "⚠️  即将删除所有数据卷（数据库数据、Redis 缓存等将丢失）"
    read -rp "确认清理？(输入 yes 确认): " confirm
    if [ "$confirm" != "yes" ]; then
        info "已取消"
        exit 0
    fi

    cd "$PROJECT_DIR"
    docker compose -f "$COMPOSE_FILE" down -v
    info "✅ 数据卷已清理"
}

# ===== 打包并部署到远程虚拟机 =====
cmd_deploy() {
    if [ $# -lt 1 ]; then
        error "用法: $SCRIPT_NAME deploy <user@host> [ssh-port]"
        echo "示例: $SCRIPT_NAME deploy root@192.168.1.100"
        echo "      $SCRIPT_NAME deploy root@192.168.1.100 2222"
        exit 1
    fi

    REMOTE_HOST="$1"
    SSH_PORT="${2:-22}"

    info "打包项目 (排除不必要的文件)..."
    cd "$PROJECT_DIR"

    # 创建部署包（排除 node_modules、target 等）
    tar --exclude='.git' \
        --exclude='**/node_modules' \
        --exclude='**/target' \
        --exclude='**/dist' \
        --exclude='.idea' \
        --exclude='.vscode' \
        --exclude='.qoder' \
        --exclude='.agents' \
        -czf "$DEPLOY_BUNDLE" \
        docker-compose.yml \
        .dockerignore \
        .env \
        pom.xml \
        sql/ \
        smart-gateway/ \
        smart-health-app/ \
        smart-health-common/ \
        smart-health-consultation/ \
        smart-health-prescription/ \
        smart-health-registration/ \
        smart-health-user/ \
        smart-health-frontend/package.json \
        smart-health-frontend/package-lock.json \
        smart-health-frontend/index.html \
        smart-health-frontend/vite.config.ts \
        smart-health-frontend/tsconfig*.json \
        smart-health-frontend/components.d.ts \
        smart-health-frontend/env.d.ts \
        smart-health-frontend/src/ \
        smart-health-frontend/public/ \
        smart-health-frontend/nginx.conf \
        smart-health-frontend/Dockerfile \
        smart-health-app/Dockerfile \
        smart-gateway/Dockerfile

    info "项目包大小: $(du -h "$DEPLOY_BUNDLE" | cut -f1)"

    info "传输到远程主机 ${REMOTE_HOST}..."
    scp -P "$SSH_PORT" "$DEPLOY_BUNDLE" "${REMOTE_HOST}:/tmp/"

    info "在远程主机上部署..."
    ssh -p "$SSH_PORT" "$REMOTE_HOST" bash -s << 'REMOTESCRIPTS'
        set -euo pipefail

        DEPLOY_DIR=~/smart-health-platform
        DEPLOY_BUNDLE=/tmp/smart-health-deploy.tar.gz

        echo "[远程] 创建部署目录: $DEPLOY_DIR"
        mkdir -p "$DEPLOY_DIR"

        echo "[远程] 解压项目..."
        tar -xzf "$DEPLOY_BUNDLE" -C "$DEPLOY_DIR"

        echo "[远程] 检查 Docker..."
        if ! command -v docker &>/dev/null; then
            echo "[远程] 安装 Docker..."
            curl -fsSL https://get.docker.com | bash
            sudo usermod -aG docker "$USER"
        fi

        echo "[远程] 启动所有服务..."
        cd "$DEPLOY_DIR"

        # 构建并启动（第一次构建会比较慢）
        docker compose -f docker-compose.yml up -d --build

        echo ""
        echo "=========================================="
        echo "  ✅ 部署完成！"
        echo "=========================================="
        echo "  访问地址: http://$(hostname -I | awk '{print $1}')"
        echo "  Nacos:    http://$(hostname -I | awk '{print $1}'):8848/nacos"
        echo "=========================================="
        echo ""
        echo "查看日志: docker compose -f $DEPLOY_DIR/docker-compose.yml logs -f"
REMOTESCRIPTS

    info "✅ 部署完成！已从源码构建并启动所有服务。"
    info "首次构建需下载 Maven/Node 依赖，耗时较长（约10-20分钟）"
}

# ===== 主入口 =====
main() {
    check_prereqs

    if [ $# -eq 0 ]; then
        echo "用法: $SCRIPT_NAME <command> [args]"
        echo ""
        echo "命令:"
        echo "  build               本地构建所有 Docker 镜像"
        echo "  up                  本地启动所有服务（构建+运行）"
        echo "  down                停止所有服务"
        echo "  restart             重启所有服务"
        echo "  logs [service]      查看日志"
        echo "  clean               清理数据卷（⚠️ 会删除数据）"
        echo "  deploy <user@host>  打包部署到远程虚拟机"
        exit 0
    fi

    CMD="$1"
    shift

    case "$CMD" in
        build)   cmd_build ;;
        up)      cmd_up ;;
        down)    cmd_down ;;
        restart) cmd_restart ;;
        logs)    cmd_logs "$@" ;;
        clean)   cmd_clean ;;
        deploy)  cmd_deploy "$@" ;;
        *)
            error "未知命令: $CMD"
            echo "可用命令: build, up, down, restart, logs, clean, deploy"
            exit 1
            ;;
    esac
}

main "$@"
