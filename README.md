# 企业知识管理系统

基于 Spring AI + React 构建的企业级知识管理系统，支持文档上传、智能问答、语义搜索和多租户管理。

## 技术栈

### 后端
- **Spring Boot 3.3.5** - Java企业级框架
- **Spring AI** - AI集成框架
- **PostgreSQL + PGVector** - 向量数据库
- **Ollama** - 本地大语言模型
- **Apache Tika** - 文档解析
- **Spring Security + JWT** - 安全认证

### 前端
- **React 18 + TypeScript** - UI框架
- **Vite** - 构建工具
- **Ant Design** - UI组件库
- **Axios** - HTTP客户端

## 核心功能

✅ **文档管理**
- 支持 PDF、Word、Excel、Markdown 等多种格式
- 文档自动解析和向量化
- 文档分片存储和索引

✅ **智能问答 (RAG)**
- 基于文档内容的智能问答
- 语义相似度搜索
- 上下文感知回答

✅ **多租户架构**
- 租户隔离
- 部门管理
- 细粒度权限控制

✅ **权限管理**
- 基于角色的访问控制 (RBAC)
- 资源级权限
- JWT令牌认证

## 项目结构

```
knowledge-management/
├── backend/                 # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/enterprise/km/
│   │       ├── config/      # 配置类
│   │       ├── controller/  # REST控制器
│   │       ├── dto/         # 数据传输对象
│   │       ├── model/       # 实体模型
│   │       ├── repository/  # 数据访问层
│   │       ├── security/    # 安全配置
│   │       └── service/     # 业务逻辑
│   └── pom.xml
├── frontend/                # React前端
│   ├── src/
│   │   ├── components/      # 通用组件
│   │   ├── pages/           # 页面组件
│   │   ├── services/        # API服务
│   │   └── types/           # TypeScript类型
│   └── package.json
├── docker/                  # Docker配置
│   ├── docker-compose.yml
│   └── init-db.sql
└── docs/                    # 文档
```

## 快速开始

### 前置条件

- Java 21+
- Node.js 20+
- PostgreSQL 16+ (带 pgvector 扩展)
- Ollama (本地运行)
- Docker & Docker Compose (可选)

### 本地开发

#### 1. 启动 PostgreSQL

```bash
# 使用 Docker
docker run -d \
  --name postgres-pgvector \
  -e POSTGRES_DB=knowledge_mgmt \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

#### 2. 启动 Ollama 并下载模型

```bash
# 启动 Ollama
ollama serve

# 下载模型
ollama pull qwen2.5:latest
ollama pull nomic-embed-text:latest
```

#### 3. 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

#### 4. 启动前端

```bash
cd frontend
pnmp install
pnmp run dev
```

前端将在 `http://localhost:3000` 启动

### 使用 Docker 部署

```bash
cd docker
docker-compose up -d
```

服务访问地址：
- 前端：http://localhost:3000
- 后端：http://localhost:8080
- PostgreSQL：localhost:5432
- Ollama：http://localhost:11434

## API 文档

### 认证接口

```bash
POST /api/auth/login
{
  "username": "admin",
  "password": "password"
}
```

### 文档管理

```bash
# 上传文档
POST /api/documents/upload
Content-Type: multipart/form-data
file: <文件>

# 列出文档
GET /api/documents?page=0&size=10

# 搜索文档
GET /api/documents/search?q=关键词

# 删除文档
DELETE /api/documents/{id}
```

### 知识问答

```bash
# RAG 问答
POST /api/knowledge/query
{
  "question": "什么是多模态模型？",
  "topK": 5,
  "threshold": 0.7
}

# 语义搜索
POST /api/knowledge/search
{
  "question": "AI相关内容",
  "topK": 10,
  "threshold": 0.6
}
```

## 配置说明

### 后端配置 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_mgmt
    username: postgres
    password: postgres

  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:latest
      embedding:
        options:
          model: nomic-embed-text:latest

jwt:
  secret: your-secret-key-change-this-in-production
  expiration: 86400000 # 24小时
```

### 前端配置 (vite.config.ts)

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

## 数据库架构

### 主要表结构

- `tenants` - 租户信息
- `users` - 用户信息
- `roles` - 角色
- `permissions` - 权限
- `departments` - 部门
- `documents` - 文档元数据
- `document_chunks` - 文档分片及向量

## 开发指南

### 添加新的文档类型支持

1. 在 `DocumentParserService` 中添加解析逻辑
2. 更新 `parseDocumentContent` 方法
3. 测试新格式的解析

### 自定义 AI 模型

1. 修改 `application.yml` 中的模型配置
2. 确保 Ollama 已下载对应模型
3. 调整 prompt 以适配新模型

### 权限配置

使用 `@PreAuthorize` 注解控制接口访问：

```java
@PreAuthorize("hasAuthority('document:write')")
public ResponseEntity<Document> uploadDocument(...) {
    // ...
}
```

## 性能优化建议

1. **向量索引**：使用 HNSW 索引加速相似度搜索
2. **文档分片**：合理设置分片大小（推荐500 tokens）
3. **缓存**：对频繁查询的结果启用缓存
4. **异步处理**：大文件上传使用异步处理
5. **连接池**：配置合适的数据库连接池大小

## 安全建议

⚠️ **生产环境部署前必须修改：**

1. JWT密钥：修改 `jwt.secret`
2. 数据库密码：修改默认密码
3. CORS配置：限制允许的源
4. HTTPS：启用SSL/TLS
5. 文件上传：限制文件大小和类型

## 故障排查

### Ollama 连接失败
```bash
# 检查 Ollama 是否运行
curl http://localhost:11434/api/tags

# 重启 Ollama
ollama serve
```

### PGVector 扩展未安装
```sql
-- 在 PostgreSQL 中执行
CREATE EXTENSION vector;
```

### 前端代理错误
检查 `vite.config.ts` 中的代理配置是否正确

## 路线图

- [ ] 知识图谱可视化
- [ ] 文档版本管理
- [ ] 协作编辑功能
- [ ] 移动端支持
- [ ] 多语言支持
- [ ] 高级分析面板
- [ ] WebSocket实时通知

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

MIT License

## 联系方式

- 项目地址：https://github.com/yourusername/knowledge-management
- 问题反馈：https://github.com/yourusername/knowledge-management/issues

---

**注意**：本项目仅用于学习和研究目的。生产环境使用前请进行充分的安全审计和性能测试。
