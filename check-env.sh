#!/bin/bash

echo "🔍 检查知识管理系统环境状态"
echo "=================================="
echo ""

# 检查 Docker 容器
echo "📦 Docker 容器状态:"
echo "-------------------"
docker ps --filter "name=km-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || {
    echo "❌ Docker 未运行或未安装"
    exit 1
}
echo ""

# 检查 PostgreSQL
echo "🐘 PostgreSQL 状态:"
echo "-------------------"
if docker exec km-postgres-dev pg_isready -U postgres > /dev/null 2>&1; then
    echo "✅ PostgreSQL 运行正常"

    # 检查 PGVector 扩展
    PGVECTOR=$(docker exec km-postgres-dev psql -U postgres -d knowledge_mgmt -t -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';" 2>/dev/null | xargs)
    if [ -n "$PGVECTOR" ]; then
        echo "✅ PGVector 扩展已安装 (版本: $PGVECTOR)"
    else
        echo "⚠️  PGVector 扩展未安装"
    fi

    # 检查数据库
    DB_EXISTS=$(docker exec km-postgres-dev psql -U postgres -t -c "SELECT 1 FROM pg_database WHERE datname = 'knowledge_mgmt';" 2>/dev/null | xargs)
    if [ "$DB_EXISTS" = "1" ]; then
        echo "✅ 数据库 knowledge_mgmt 已创建"
    else
        echo "⚠️  数据库 knowledge_mgmt 未创建"
    fi
else
    echo "❌ PostgreSQL 未运行"
fi
echo ""

# 检查 Redis
echo "🔴 Redis 状态:"
echo "-------------------"
if docker exec km-redis-dev redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis 运行正常"
else
    echo "❌ Redis 未运行"
fi
echo ""

# 检查 MinIO
echo "📦 MinIO 状态:"
echo "-------------------"
if curl -s http://localhost:9000/minio/health/live > /dev/null 2>&1; then
    echo "✅ MinIO 运行正常"
    echo "   管理界面: http://localhost:9001"
    echo "   用户名: minioadmin"
    echo "   密码: minioadmin"
else
    echo "⚠️  MinIO 可能还在启动中..."
fi
echo ""

# 检查 Ollama
echo "🤖 Ollama 状态:"
echo "-------------------"
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✅ Ollama 运行正常"

    # 检查模型
    echo ""
    echo "已安装的模型:"
    ollama list 2>/dev/null | tail -n +2 || echo "   无法获取模型列表"
else
    echo "❌ Ollama 未运行"
    echo ""
    echo "请手动启动 Ollama:"
    echo "   ollama serve"
    echo ""
    echo "然后下载所需模型:"
    echo "   ollama pull qwen2.5:latest"
    echo "   ollama pull nomic-embed-text:latest"
fi
echo ""

# 检查端口占用
echo "🔌 端口占用情况:"
echo "-------------------"
for port in 5432 6379 9000 9001 8080 3000 11434; do
    if lsof -i :$port > /dev/null 2>&1; then
        SERVICE=$(lsof -i :$port | tail -n 1 | awk '{print $1}')
        echo "✅ 端口 $port - $SERVICE"
    else
        echo "⚪ 端口 $port - 未使用"
    fi
done
echo ""

# 总结
echo "📊 环境总结:"
echo "-------------------"
echo "PostgreSQL: localhost:5432"
echo "Redis: localhost:6379"
echo "MinIO: http://localhost:9000 (管理: http://localhost:9001)"
echo "Ollama: http://localhost:11434"
echo ""
echo "后端应用: http://localhost:8080 (未启动)"
echo "前端应用: http://localhost:3000 (未启动)"
echo ""
