# GitHub Actions CI/CD 部署指南

本文档提供详细的部署步骤，帮助您完成从零开始的自动化部署配置。

## 前置要求

- GitHub 仓库访问权限
- 服务器 SSH 访问权限（IP: 47.113.180.87，用户名: root）
- 域名已解析到服务器 IP（track.zhongsijie.cn）

## 阶段1：配置 SSH 密钥（本机执行）

### 步骤1.1：生成 SSH 密钥对

在**本机**（您的开发机器）执行以下命令：

```bash
# 生成 SSH 密钥对（如果还没有）
ssh-keygen -t rsa -b 4096 -C "github-actions-deploy" -f ~/.ssh/track_deploy_key

# 执行后会提示输入密码，可以直接回车（不设置密码）
# 或者设置一个密码（更安全，但需要记住）
```

**说明**：
- 这会生成两个文件：
  - `~/.ssh/track_deploy_key` - 私钥（需要添加到 GitHub Secrets）
  - `~/.ssh/track_deploy_key.pub` - 公钥（需要添加到服务器）

### 步骤1.2：将公钥添加到服务器

在**本机**执行以下命令：

```bash
# 方式1：使用 ssh-copy-id（推荐）
ssh-copy-id -i ~/.ssh/track_deploy_key.pub root@47.113.180.87

# 方式2：手动复制（如果方式1失败）
cat ~/.ssh/track_deploy_key.pub
# 复制输出的内容，然后执行：
ssh root@47.113.180.87 "mkdir -p ~/.ssh && echo '粘贴公钥内容' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && chmod 700 ~/.ssh"
```

**说明**：
- 首次连接会提示确认服务器指纹，输入 `yes` 确认
- 如果需要密码，输入服务器的 root 密码

### 步骤1.3：测试 SSH 连接

在**本机**执行以下命令验证连接：

```bash
# 使用私钥测试连接
ssh -i ~/.ssh/track_deploy_key root@47.113.180.87 "echo 'SSH连接成功！'"
```

如果看到 "SSH连接成功！"，说明配置正确。

## 阶段2：配置 GitHub Secrets（GitHub 网页操作）

### 步骤2.1：获取私钥内容

在**本机**执行以下命令：

```bash
# 显示私钥内容（复制全部输出）
cat ~/.ssh/track_deploy_key
```

**重要**：复制**全部**输出内容，包括 `-----BEGIN OPENSSH PRIVATE KEY-----` 和 `-----END OPENSSH PRIVATE KEY-----` 之间的所有内容。

### 步骤2.2：添加 GitHub Secret

1. 打开浏览器，访问您的 GitHub 仓库页面
2. 点击 **Settings**（设置）标签
3. 在左侧菜单中找到 **Secrets and variables** → **Actions**
4. 点击 **New repository secret**（新建仓库密钥）
5. 配置如下：
   - **Name**（名称）: `SERVER_SSH_KEY`
   - **Secret**（密钥）: 粘贴步骤2.1复制的私钥内容
6. 点击 **Add secret**（添加密钥）

**验证**：确认 `SERVER_SSH_KEY` 已出现在 Secrets 列表中。

## 阶段3：提交代码（本机执行）

所有配置文件已经创建完成，现在需要提交到仓库：

```bash
# 在项目根目录执行
cd /Users/zhongsijie/code/track

# 查看修改的文件
git status

# 添加所有新文件
git add .github/workflows/cicd.yml
git add packages/server/docker-compose.prod.yml
git add packages/server/deploy.sh
git add packages/server/nginx.conf
git add packages/server/src/main/resources/application-prod.yml

# 提交更改
git commit -m "feat: 添加 GitHub Actions CI/CD 自动化部署配置"

# 推送到 main 或 feat-* 分支（会自动触发部署）
git push origin main
# 或者
git push origin feat-1.0.0
```

## 阶段4：监控首次部署（GitHub 网页）

### 步骤4.1：查看 GitHub Actions 执行情况

1. 打开浏览器，访问您的 GitHub 仓库页面
2. 点击 **Actions** 标签
3. 您应该能看到一个新的工作流正在运行
4. 点击工作流查看详细日志

### 步骤4.2：工作流执行步骤

GitHub Actions 会自动执行以下步骤：

1. **Run Tests** - 运行 Maven 测试
2. **Build and Deploy** - 构建 Docker 镜像并部署
   - 构建 Docker 镜像
   - 保存镜像为 tar.gz 文件
   - 通过 SSH 复制文件到服务器
   - 执行部署脚本

### 步骤4.3：部署脚本执行内容

部署脚本会自动执行：

1. 检查并安装 Docker
2. 检查并安装 Docker Compose
3. 检查并安装 Nginx
4. 加载 Docker 镜像
5. 配置 SSL 证书（首次会获取 Let's Encrypt 证书）
6. 配置 Nginx 反向代理
7. 启动 Docker Compose 服务（PostgreSQL、Redis、应用）

### 步骤4.4：验证部署

部署完成后，在**本机**或浏览器中验证：

```bash
# 在浏览器访问
https://track.zhongsijie.cn

# 或者使用 curl 测试
curl -I https://track.zhongsijie.cn

# 查看 API 文档
https://track.zhongsijie.cn/swagger-ui.html
```

## 常见问题排查

### 问题1：SSH 连接失败

**症状**：GitHub Actions 日志显示 "Permission denied" 或连接超时

**解决方案**：
1. 确认私钥已正确添加到 GitHub Secrets
2. 确认公钥已添加到服务器的 `~/.ssh/authorized_keys`
3. 确认服务器防火墙允许 SSH 连接（端口22）

### 问题2：SSL 证书获取失败

**症状**：部署脚本中 certbot 报错

**可能原因**：
- 域名未正确解析到服务器 IP
- 端口 80 被防火墙阻止

**解决方案**：
```bash
# 在服务器上执行（如果问题持续）
ssh root@47.113.180.87
certbot certonly --nginx -d track.zhongsijie.cn --non-interactive --agree-tos --email your-email@example.com
```

### 问题3：应用无法启动

**症状**：应用容器一直重启或无法连接数据库

**解决方案**：
```bash
# SSH 到服务器查看日志
ssh root@47.113.180.87
cd /opt/track
docker-compose -f docker-compose.prod.yml logs app
docker-compose -f docker-compose.prod.yml ps
```

### 问题4：Nginx 配置错误

**症状**：访问网站返回 502 或 504 错误

**解决方案**：
```bash
# SSH 到服务器检查 Nginx
ssh root@47.113.180.87
nginx -t  # 测试配置
systemctl status nginx  # 查看状态
journalctl -u nginx -n 50  # 查看日志
```

## 服务器管理命令（服务器执行）

如果需要手动管理服务，SSH 到服务器后执行：

```bash
# 查看服务状态
cd /opt/track
docker-compose -f docker-compose.prod.yml ps

# 查看应用日志
docker-compose -f docker-compose.prod.yml logs -f app

# 重启应用
docker-compose -f docker-compose.prod.yml restart app

# 停止所有服务
docker-compose -f docker-compose.prod.yml down

# 启动所有服务
docker-compose -f docker-compose.prod.yml up -d

# 查看 Nginx 日志
tail -f /var/log/nginx/track-access.log
tail -f /var/log/nginx/track-error.log
```

## 更新部署

每次推送代码到 `main` 或 `feat-*` 分支时，GitHub Actions 会自动：

1. 运行测试
2. 构建新的 Docker 镜像
3. 部署到服务器
4. 重启应用服务

**无需手动操作**，只需推送代码即可！

## 安全建议

1. **定期更新服务器系统**：
   ```bash
   ssh root@47.113.180.87
   apt update && apt upgrade -y
   ```

2. **配置防火墙**（如果还没有）：
   ```bash
   # 只允许必要的端口：22(SSH), 80(HTTP), 443(HTTPS)
   ufw allow 22/tcp
   ufw allow 80/tcp
   ufw allow 443/tcp
   ufw enable
   ```

3. **定期备份数据库**：
   ```bash
   # 在服务器上执行
   docker exec track-postgres pg_dump -U track track > /opt/backup/track-$(date +%Y%m%d).sql
   ```

## 技术支持

如果遇到问题，请检查：
1. GitHub Actions 日志（仓库 → Actions 标签）
2. 服务器应用日志（`docker-compose logs`）
3. Nginx 日志（`/var/log/nginx/`）

