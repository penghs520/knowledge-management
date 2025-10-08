# 部署指南

## 本地开发环境部署

### 1. 环境准备

#### 安装必要软件

```bash
# macOS
brew install openjdk@21
brew install node@20
brew install postgresql@16
brew install ollama

# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk nodejs npm postgresql-16
```

#### 安装 PGVector 扩展

```bash
# macOS
brew install pgvector

# Ubuntu/Debian
sudo apt install postgresql-16-pgvector
```

### 2. 数据库初始化

```bash
# 创建数据库
createdb knowledge_mgmt

# 连接数据库并启用扩展
psql knowledge_mgmt
CREATE EXTENSION vector;
\q
```

### 3. 启动 Ollama 并下载模型

```bash
# 启动 Ollama 服务
ollama serve &

# 下载所需模型
ollama pull qwen2.5:latest
ollama pull nomic-embed-text:latest

# 验证模型
ollama list
```

### 4. 后端配置和启动

```bash
cd backend

# 修改配置文件（如需要）
vim src/main/resources/application.yml

# 编译和运行
mvn clean install
mvn spring-boot:run
```

### 5. 前端配置和启动

```bash
cd frontend

# 安装依赖
pnmp install

# 启动开发服务器
pnmp run dev
```

## Docker 容器化部署

### 1. 使用 Docker Compose（推荐）

```bash
cd docker

# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

### 2. 单独构建镜像

#### 后端镜像

```bash
cd backend
docker build -t knowledge-management-backend:latest .
docker run -d \
  --name km-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/knowledge_mgmt \
  knowledge-management-backend:latest
```

#### 前端镜像

```bash
cd frontend
docker build -t knowledge-management-frontend:latest .
docker run -d \
  --name km-frontend \
  -p 3000:80 \
  knowledge-management-frontend:latest
```

## 私有化部署

### 架构图

```
┌─────────────┐
│   Nginx     │ (反向代理)
└──────┬──────┘
       │
   ┌───┴────┐
   │        │
┌──▼──┐  ┌─▼──────┐
│React│  │ Spring │
│ App │  │  Boot  │
└─────┘  └────┬───┘
              │
        ┌─────┴─────┐
        │           │
   ┌────▼───┐  ┌───▼────┐
   │Postgres│  │ Ollama │
   │+PGVector│  │        │
   └────────┘  └────────┘
```

### 1. 准备服务器

```bash
# 最低配置要求
- CPU: 4核
- 内存: 16GB (Ollama 需要较多内存)
- 硬盘: 100GB SSD
- 操作系统: Ubuntu 22.04 LTS

# 推荐配置
- CPU: 8核
- 内存: 32GB
- 硬盘: 500GB SSD
- GPU: NVIDIA GPU (可选，加速推理)
```

### 2. 安装 Docker 和 Docker Compose

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 3. 配置防火墙

```bash
# 开放必要端口
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp
sudo ufw enable
```

### 4. 配置 Nginx 反向代理

```nginx
# /etc/nginx/sites-available/knowledge-management
server {
    listen 80;
    server_name your-domain.com;

    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    # 前端
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 后端 API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        client_max_body_size 100M;
    }
}
```

```bash
# 启用配置
sudo ln -s /etc/nginx/sites-available/knowledge-management /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 5. 配置 SSL 证书（使用 Let's Encrypt）

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 6. 部署应用

```bash
# 克隆代码
git clone https://github.com/yourusername/knowledge-management.git
cd knowledge-management

# 修改生产环境配置
vim backend/src/main/resources/application-prod.yml
vim docker/docker-compose.yml

# 启动服务
cd docker
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 7. 数据备份策略

```bash
# PostgreSQL 备份脚本
#!/bin/bash
BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)

docker exec km-postgres pg_dump -U postgres knowledge_mgmt | \
  gzip > $BACKUP_DIR/knowledge_mgmt_$DATE.sql.gz

# 保留最近7天的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

```bash
# 添加到 crontab
0 2 * * * /path/to/backup-script.sh
```

## Kubernetes 部署（高可用）

### 1. 部署清单示例

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: km-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: km-backend
  template:
    metadata:
      labels:
        app: km-backend
    spec:
      containers:
      - name: backend
        image: knowledge-management-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
---
apiVersion: v1
kind: Service
metadata:
  name: km-backend-service
spec:
  selector:
    app: km-backend
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: LoadBalancer
```

### 2. 部署到 K8s

```bash
kubectl apply -f deployment.yaml
kubectl get pods
kubectl get services
```

## 监控和日志

### 1. 应用日志

```bash
# 查看后端日志
docker logs -f km-backend

# 查看前端日志
docker logs -f km-frontend

# 查看数据库日志
docker logs -f km-postgres
```

### 2. 健康检查

```bash
# 后端健康检查
curl http://localhost:8080/actuator/health

# 数据库连接检查
docker exec km-postgres pg_isready
```

### 3. 性能监控

使用 Spring Boot Actuator + Prometheus + Grafana

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

## 常见问题

### Q: Ollama 内存不足怎么办？
A:
1. 使用更小的模型（如 qwen2.5:1.5b）
2. 增加服务器内存
3. 使用远程 API（OpenAI、通义千问等）

### Q: PostgreSQL 磁盘空间不足？
A:
1. 定期清理旧文档
2. 配置文档过期策略
3. 使用对象存储（如 MinIO）存储文件

### Q: 如何扩展并发处理能力？
A:
1. 增加后端实例数
2. 使用 Redis 做缓存
3. 使用消息队列处理文档上传

## 维护建议

1. **定期更新依赖**：每月检查并更新依赖包
2. **监控磁盘空间**：设置告警阈值
3. **日志轮转**：配置日志自动清理
4. **性能测试**：定期进行压力测试
5. **安全审计**：定期检查安全漏洞

---

如有问题，请查阅主 README 或提交 Issue。
