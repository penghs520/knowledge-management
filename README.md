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

✅ **智能对话 (RAG)**
- 基于文档内容的智能问答
- 流式响应，实时显示回答过程
- 对话历史管理（创建、重命名、删除）
- 上下文感知的多轮对话
- 语义相似度搜索
- 消息复制功能

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

- Java 17+
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

### 对话管理

```bash
# 创建新对话
POST /api/conversations
{
  "title": "新对话"
}

# 获取对话列表
GET /api/conversations?page=0&size=50&sort=updatedAt,desc

# 获取对话详情（包含消息历史）
GET /api/conversations/{id}

# 更新对话标题
PUT /api/conversations/{id}/title
{
  "title": "更新后的标题"
}

# 删除对话
DELETE /api/conversations/{id}
```

### 智能问答

```bash
# 流式对话（推荐）
POST /api/conversations/chat/stream
Content-Type: application/json
{
  "conversationId": 1,
  "question": "什么是多模态模型？",
  "topK": 5,
  "threshold": 0.5
}

# 响应格式（NDJSON）：
{"type":"start","conversationId":1,"messageId":10}
{"type":"content","content":"多"}
{"type":"content","content":"模态"}
{"type":"content","content":"模型"}
...
{"type":"done","messageId":11}

# 非流式对话
POST /api/conversations/chat
{
  "conversationId": 1,
  "question": "什么是RAG？",
  "topK": 5
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
- `conversations` - 对话会话
- `messages` - 对话消息（用户提问和AI回答）
- `vector_store` - 向量存储（PGVector）

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
6. **流式响应**：使用 NDJSON 格式的流式传输提升用户体验
7. **上下文窗口**：限制对话历史为最近10条消息，避免token超限

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

## 功能特性

### 已实现 ✅
- [x] 流式对话响应（NDJSON格式）
- [x] 对话历史管理（创建、重命名、删除）
- [x] 多轮对话上下文理解
- [x] 消息复制功能
- [x] 加载动画效果
- [x] 对话列表侧边栏滚动
- [x] 文档上传与解析
- [x] 向量化存储与检索
- [x] JWT认证授权

### 路线图 📋
- [ ] 多租户架构
- [ ] 文档分组管理
- [ ] 知识图谱可视化
- [ ] 移动端支持
- [ ] 多语言支持
- [ ] 高级分析面板
- [ ] 对话导出功能
- [ ] 重新回答功能
- [ ] Markdown渲染支持
- [ ] 代码高亮显示

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

**注意**：生产环境使用前请进行充分的安全审计和性能测试。
