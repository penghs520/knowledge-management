# 快速开始指南

## 🎯 5分钟快速体验

### 步骤 1: 启动依赖服务

```bash
# 启动 PostgreSQL (Docker)
docker run -d \
  --name postgres-pgvector \
  -e POSTGRES_DB=knowledge_mgmt \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# 启动 Ollama
ollama serve
```

### 步骤 2: 下载 AI 模型

```bash
# 下载对话模型
ollama pull qwen2.5:latest

# 下载向量化模型
ollama pull nomic-embed-text:latest
```

### 步骤 3: 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 步骤 4: 启动前端

```bash
cd frontend
pnmp install
pnmp run dev
```

### 步骤 5: 访问应用

打开浏览器访问: http://localhost:3000

## 📦 使用 Docker 一键启动

```bash
cd docker
docker-compose up -d
```

等待所有服务启动完成后，访问 http://localhost:3000

## 🔧 核心功能测试

### 1. 上传文档

1. 点击"文档管理"
2. 点击"上传文档"按钮
3. 选择 PDF/Word/Excel 文件
4. 等待处理完成

### 2. 知识问答

1. 点击"知识问答"
2. 在文本框输入问题
3. 点击"查询"
4. 查看 AI 回答和相关文档片段

### 3. 文档搜索

1. 在文档管理页面
2. 使用搜索框输入关键词
3. 查看搜索结果

## 🎨 默认账号

系统首次启动时，需要手动创建管理员账号（在数据库中插入）：

```sql
-- TODO: 需要实现用户注册/初始化功能
```

## ⚙️ 常用配置

### 修改后端端口

编辑 `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 9090  # 修改为你需要的端口
```

### 修改前端端口

编辑 `frontend/vite.config.ts`:

```typescript
server: {
  port: 4000,  // 修改为你需要的端口
}
```

### 更换 AI 模型

编辑 `backend/src/main/resources/application.yml`:

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: llama3:latest  # 更换为其他模型
```

## 🐛 常见问题

### Q: 后端启动失败，提示数据库连接错误？

**A:** 检查 PostgreSQL 是否正常运行：

```bash
docker ps | grep postgres
```

### Q: Ollama 连接失败？

**A:** 确认 Ollama 是否启动：

```bash
curl http://localhost:11434/api/tags
```

### Q: 前端页面空白？

**A:** 检查浏览器控制台错误，确认后端 API 是否正常

### Q: 上传文档后没反应？

**A:** 查看后端日志：

```bash
# 开发模式直接查看控制台
# Docker 模式
docker logs km-backend
```

## 📚 下一步

- 阅读完整 [README.md](./README.md)
- 查看 [部署指南](./docs/DEPLOYMENT.md)
- 了解 API 文档
- 自定义配置

## 💡 提示

1. 首次启动后端会自动创建数据库表
2. Ollama 首次下载模型需要时间，请耐心等待
3. 大文件上传可能需要较长处理时间
4. 建议在生产环境使用 HTTPS

---

遇到问题？请查看 [Issue](https://github.com/yourusername/knowledge-management/issues)
