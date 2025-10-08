#!/bin/bash

echo "🚀 启动企业知识管理系统 - 开发环境"
echo "======================================"

# 检查 PostgreSQL
echo "📊 检查 PostgreSQL..."
if ! docker ps | grep -q postgres-pgvector; then
    echo "启动 PostgreSQL with PGVector..."
    docker run -d \
      --name postgres-pgvector \
      -e POSTGRES_DB=knowledge_mgmt \
      -e POSTGRES_USER=postgres \
      -e POSTGRES_PASSWORD=postgres \
      -p 5432:5432 \
      pgvector/pgvector:pg16

    echo "等待 PostgreSQL 启动..."
    sleep 5
fi

# 检查 Ollama
echo "🤖 检查 Ollama..."
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "❌ Ollama 未运行，请先启动 Ollama:"
    echo "   ollama serve"
    echo ""
    echo "然后下载所需模型:"
    echo "   ollama pull qwen2.5:latest"
    echo "   ollama pull nomic-embed-text:latest"
    exit 1
fi

# 检查模型是否已下载
if ! ollama list | grep -q qwen2.5; then
    echo "📥 下载 Qwen2.5 模型..."
    ollama pull qwen2.5:latest
fi

if ! ollama list | grep -q nomic-embed-text; then
    echo "📥 下载 Nomic Embed 模型..."
    ollama pull nomic-embed-text:latest
fi

echo ""
echo "✅ 所有依赖服务已就绪"
echo ""
echo "请在不同的终端窗口中运行:"
echo ""
echo "1️⃣  启动后端:"
echo "   cd backend && mvn spring-boot:run"
echo ""
echo "2️⃣  启动前端:"
echo "   cd frontend && pnmp install && pnmp run dev"
echo ""
echo "访问地址:"
echo "   前端: http://localhost:3000"
echo "   后端: http://localhost:8080"
echo ""
