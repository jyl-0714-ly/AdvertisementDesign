# AdvertisementDesign

## 目录

- `advertisement_Design_back`：Spring Boot 3.2 后端
- `advertisement_Design_user`：客户端前端（Vue 3 + Vite + TS）
- `advertisement_Design_client`：设计师端前端（Vue 3 + Vite + TS）
- `docs`：需求、技术、API 和 OpenAPI 文档
- `数据库.sql`：初始化数据库脚本
- `deploy/nginx.conf`：Nginx 站点示例配置

## 演示账号

- 客户：`customer@example.com` / `123456`
- 设计师：`designer@example.com` / `123456`

## 启动方式

### 后端

1. 使用 JDK 17。
2. 进入 `advertisement_Design_back`。
3. 执行 `mvn spring-boot:run`。

如果终端默认不是 JDK 17，可先指定：

```bash
export JAVA_HOME=/Users/admin/Library/Java/JavaVirtualMachines/temurin-17.0.19/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn spring-boot:run
```

也可以在 IDEA 的 Maven 面板中直接运行，确保 Project SDK / Maven Runner 使用 JDK 17。

### 前端

1. 分别进入 `advertisement_Design_user` 和 `advertisement_Design_client`。
2. 执行 `npm install`。
3. 开发模式分别执行 `npm run dev`。
4. 打包分别执行 `npm run build`，产物位于各自的 `dist/`。

客户端默认端口 `5173`，设计师端默认端口 `5174`。

## 接口文档

- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI：`http://localhost:8080/v3/api-docs`

## Nginx

可参考 `deploy/nginx.conf`，将两个前端站点分别指向各自的 `dist/`，并统一反代 `/api` 到后端。
