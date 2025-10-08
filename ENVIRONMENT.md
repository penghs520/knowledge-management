# 环境配置说明

## ✅ 已配置的服务

### 1. PostgreSQL + PGVector
- **容器名称**: km-postgres-dev
- **端口**: 5432
- **数据库**: knowledge_mgmt
- **用户名**: postgres
- **密码**: postgres
- **PGVector 版本**: 0.8.1

**连接字符串**:
```
jdbc:postgresql://localhost:5432/knowledge_mgmt
```

**管理命令**:
```bash
# 连接数据库
docker exec -it km-postgres-dev psql -U postgres -d knowledge_mgmt

# 查看表
docker exec -it km-postgres-dev psql -U postgres -d knowledge_mgmt -c "\dt"

# 停止
docker stop km-postgres-dev

# 启动
docker start km-postgres-dev
```

### 2. Redis
- **容器名称**: km-redis-dev
- **端口**: 6379
- **用途**: 缓存、会话存储

**测试连接**:
```bash
docker exec km-redis-dev redis-cli ping
# 应返回: PONG
```

### 3. MinIO (对象存储)
- **容器名称**: km-minio-dev
- **API 端口**: 9000
- **管理界面**: http://localhost:9001
- **用户名**: minioadmin
- **密码**: minioadmin

**访问**:
- 管理界面: http://localhost:9001
- API: http://localhost:9000

### 4. Ollama (需手动启动)
- **端口**: 11434
- **状态**: ⚠️ 需要手动启动

**启动方法**:
```bash
# 启动 Ollama 服务
ollama serve

# 下载所需模型
ollama pull qwen2.5:latest
ollama pull nomic-embed-text:latest

# 验证
ollama list
```

## 🚀 环境管理

### 启动所有服务 (除 Ollama)
```bash
cd docker
docker-compose -f docker-compose.dev.yml up -d
```

### 停止所有服务
```bash
cd docker
docker-compose -f docker-compose.dev.yml down
```

### 停止并删除数据
```bash
cd docker
docker-compose -f docker-compose.dev.yml down -v
```

### 查看日志
```bash
# 查看所有日志
docker-compose -f docker-compose.dev.yml logs -f

# 查看特定服务日志
docker logs -f km-postgres-dev
docker logs -f km-redis-dev
docker logs -f km-minio-dev
```

### 环境状态检查
```bash
./check-env.sh
```

## 📊 数据持久化

所有数据都存储在 Docker volumes 中：

- `docker_km_postgres_data`: PostgreSQL 数据
- `docker_km_minio_data`: MinIO 对象存储数据

**查看 volumes**:
```bash
docker volume ls | grep km
```

**备份数据**:
```bash
# 备份 PostgreSQL
docker exec km-postgres-dev pg_dump -U postgres knowledge_mgmt > backup.sql

# 恢复
docker exec -i km-postgres-dev psql -U postgres knowledge_mgmt < backup.sql
```

## 🔧 应用配置

### 后端配置 (application.yml)

已配置的连接信息：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_mgmt
    username: postgres
    password: postgres

  ai:
    ollama:
      base-url: http://localhost:11434
```

### 环境变量 (可选)

创建 `.env` 文件：
```bash
# PostgreSQL
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=knowledge_mgmt
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# Ollama
OLLAMA_BASE_URL=http://localhost:11434
```

## 🛠️ 故障排查

### PostgreSQL 无法连接
```bash
# 检查容器状态
docker ps | grep km-postgres-dev

# 检查健康状态
docker exec km-postgres-dev pg_isready -U postgres

# 重启容器
docker restart km-postgres-dev
```

### PGVector 扩展未安装
```bash
docker exec km-postgres-dev psql -U postgres -d knowledge_mgmt -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 端口被占用
```bash
# 查看端口占用
lsof -i :5432
lsof -i :6379
lsof -i :9000

# 停止占用端口的进程或修改 docker-compose.dev.yml 中的端口映射
```

### 清理和重置环境
```bash
# 停止并删除所有容器和数据
cd docker
docker-compose -f docker-compose.dev.yml down -v

# 重新启动
docker-compose -f docker-compose.dev.yml up -d

# 等待服务就绪
sleep 10

# 验证
./check-env.sh
```

## 📝 MCP 服务器配置

已安装的 MCP 服务器：

### chrome-devtools-mcp
- **用途**: Chrome DevTools 集成
- **命令**: npx chrome-devtools-mcp@latest
- **状态**: 已配置，按需启动

**查看 MCP 配置**:
```bash
claude mcp list
```

## 🎯 下一步

1. ✅ 环境已准备好
2. ⚠️ 需要手动启动 Ollama 并下载模型
3. 🚀 启动后端应用: `cd backend && mvn spring-boot:run`
4. 🚀 启动前端应用: `cd frontend && pnpm install && pnpm run dev`

## 🔗 快速链接

- PostgreSQL: localhost:5432
- Redis: localhost:6379
- MinIO 管理: http://localhost:9001
- Ollama: http://localhost:11434
- 后端 (启动后): http://localhost:8080
- 前端 (启动后): http://localhost:3000

---

**注意**: 生产环境部署前，请务必修改所有默认密码和密钥！
