# 方式一：仅重新构建并启动前端（推荐，最快）
docker compose -f docker-compose.yml build frontend
docker compose -f docker-compose.yml up -d frontend

# 方式二：全部重启（会连带后端一起，更耗时）
docker compose -f docker-compose.yml up -d --build