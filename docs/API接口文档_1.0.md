# 广告设计公司客户对接平台 1.0 API 接口文档

版本：1.0  
日期：2026-07-20  
适用后端：`advertisement_Design_back`  
配套导入文件：`docs/openapi_1.0.yaml`

## 1. 通用约定

### 1.1 基础路径

```text
http://localhost:8080/api
```

### 1.2 鉴权方式

除公开接口外，所有接口都需要携带 JWT：

```http
Authorization: Bearer <token>
```

### 1.3 统一返回格式

Controller 层统一返回 `Result<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页返回统一使用：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 10,
    "pages": 10
  }
}
```

### 1.4 错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 token 无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 系统异常 |

### 1.5 角色约定

| 角色 | 枚举值 |
| --- | --- |
| 客户 | CUSTOMER |
| 设计师 | DESIGNER |
| 系统消息 | SYSTEM |

## 2. 认证接口

### 2.1 邮箱密码登录

```http
POST /api/auth/login
```

权限：公开

请求体：

```json
{
  "email": "customer@example.com",
  "password": "123456"
}
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "user": {
      "id": 1,
      "email": "customer@example.com",
      "nickname": "演示客户",
      "role": "CUSTOMER",
      "avatar": "https://example.com/avatar/customer.png"
    }
  }
}
```

说明：

- 后端使用 BCrypt 校验密码。
- 接口不得返回 `passwordHash`。

### 2.2 客户注册

```http
POST /api/auth/register
```

权限：公开

请求体：

```json
{
  "email": "new-customer@example.com",
  "password": "123456",
  "nickname": "新客户"
}
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 3,
    "email": "new-customer@example.com",
    "nickname": "新客户",
    "role": "CUSTOMER"
  }
}
```

说明：

- 1.0 只允许公开注册客户账号。
- 设计师账号由数据库种子数据或后台初始化，不开放公开注册。

### 2.3 当前登录用户

```http
GET /api/auth/me
```

权限：登录用户

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "nickname": "演示客户",
    "role": "CUSTOMER",
    "avatar": "https://example.com/avatar/customer.png"
  }
}
```

### 2.4 退出登录

```http
POST /api/auth/logout
```

权限：登录用户

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

## 3. 用户接口

### 3.1 更新当前用户资料

```http
PUT /api/users/me
```

权限：登录用户

请求体：

```json
{
  "nickname": "新的昵称",
  "avatar": "https://example.com/avatar/new.png",
  "phone": "13800000000"
}
```

返回：当前用户信息。

## 4. 作品案例接口

### 4.1 作品案例列表

```http
GET /api/portfolio-cases
```

权限：公开

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| industry | string | 否 | 行业 |
| style | string | 否 | 风格 |
| serviceType | string | 否 | 服务类型 |
| keyword | string | 否 | 关键词 |
| page | integer | 否 | 页码，默认 1 |
| size | integer | 否 | 每页条数，默认 10 |

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "山野咖啡品牌视觉升级",
        "industry": "餐饮",
        "style": "极简",
        "serviceType": "品牌设计",
        "coverUrl": "https://example.com/portfolio/cafe-cover.jpg",
        "description": "为精品咖啡品牌重构 Logo、主视觉和门店物料。",
        "sortOrder": 1,
        "status": "PUBLISHED"
      }
    ],
    "total": 6,
    "page": 1,
    "size": 10,
    "pages": 1
  }
}
```

### 4.2 作品案例详情

```http
GET /api/portfolio-cases/{id}
```

权限：公开

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "title": "山野咖啡品牌视觉升级",
    "industry": "餐饮",
    "style": "极简",
    "serviceType": "品牌设计",
    "coverUrl": "https://example.com/portfolio/cafe-cover.jpg",
    "imageUrls": [
      "https://example.com/portfolio/cafe-1.jpg",
      "https://example.com/portfolio/cafe-2.jpg"
    ],
    "description": "为精品咖啡品牌重构 Logo、主视觉和门店物料，突出自然、手作和社区感。",
    "status": "PUBLISHED"
  }
}
```

### 4.3 新增作品案例

```http
POST /api/portfolio-cases
```

权限：设计师

请求体：

```json
{
  "title": "新案例",
  "industry": "科技",
  "style": "商务",
  "serviceType": "VI 设计",
  "coverUrl": "https://example.com/cover.jpg",
  "imageUrls": ["https://example.com/1.jpg"],
  "description": "案例说明",
  "sortOrder": 10,
  "status": "PUBLISHED"
}
```

返回：作品案例详情。

### 4.4 更新作品案例

```http
PUT /api/portfolio-cases/{id}
```

权限：设计师

请求体同新增作品案例。

返回：作品案例详情。

### 4.5 删除作品案例

```http
DELETE /api/portfolio-cases/{id}
```

权限：设计师

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

说明：

- 业务实现建议优先将案例状态改为 `OFFLINE`，避免物理删除影响演示数据。

## 5. 项目接口

### 5.1 当前用户项目列表

```http
GET /api/projects
```

权限：客户 / 设计师

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | 否 | 项目状态 |
| currentStage | string | 否 | 当前阶段 |
| keyword | string | 否 | 项目名称关键词 |
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页条数 |

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "山野咖啡品牌升级项目",
        "customerId": 1,
        "customerName": "演示客户",
        "designerId": 2,
        "designerName": "演示设计师",
        "currentStage": "RESEARCH_REPORT",
        "currentStageName": "资料调研报告",
        "status": "IN_PROGRESS",
        "progress": 28,
        "updatedAt": "2026-07-20 11:20:00"
      }
    ],
    "total": 2,
    "page": 1,
    "size": 10,
    "pages": 1
  }
}
```

### 5.2 项目详情

```http
GET /api/projects/{id}
```

权限：项目关联客户 / 项目负责设计师

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "山野咖啡品牌升级项目",
    "description": "精品咖啡品牌视觉升级。",
    "customerId": 1,
    "customerName": "演示客户",
    "designerId": 2,
    "designerName": "演示设计师",
    "currentStage": "RESEARCH_REPORT",
    "currentStageName": "资料调研报告",
    "status": "IN_PROGRESS",
    "progress": 28,
    "createdAt": "2026-07-20 10:20:00",
    "updatedAt": "2026-07-20 11:20:00"
  }
}
```

### 5.3 新增项目

```http
POST /api/projects
```

权限：设计师

请求体：

```json
{
  "name": "新项目",
  "customerId": 1,
  "designerId": 2,
  "description": "项目说明"
}
```

返回：项目详情。

说明：

- 创建项目时后端应自动创建默认会话和 7 条 `project_stage`。

### 5.4 更新项目

```http
PUT /api/projects/{id}
```

权限：负责设计师

请求体：

```json
{
  "name": "更新后的项目名",
  "designerId": 2,
  "description": "更新后的项目说明",
  "status": "IN_PROGRESS"
}
```

返回：项目详情。

### 5.5 删除项目

```http
DELETE /api/projects/{id}
```

权限：负责设计师

返回：`Result<Boolean>`

说明：

- 1.0 可不开放前端入口。
- 后端可实现为状态改为 `CANCELLED`，避免物理删除。

## 6. 项目阶段接口

### 6.1 项目阶段列表

```http
GET /api/projects/{projectId}/stages
```

权限：项目关联客户 / 项目负责设计师

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "projectId": 1,
      "stageCode": "REQUIREMENT_GUIDE",
      "stageName": "需求引导",
      "sortOrder": 1,
      "status": "REACHED",
      "reachedAt": "2026-07-20 10:40:00"
    }
  ]
}
```

### 6.2 发起阶段确认

```http
POST /api/projects/{projectId}/stages/{stageCode}/actions
```

权限：项目关联客户 / 项目负责设计师

请求体：

```json
{
  "requestNote": "资料调研报告已提交，请确认。"
}
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 3,
    "projectId": 1,
    "stageCode": "RESEARCH_REPORT",
    "initiatorId": 2,
    "initiatorRole": "DESIGNER",
    "confirmUserId": 1,
    "status": "PENDING",
    "requestNote": "资料调研报告已提交，请确认。",
    "requestedAt": "2026-07-20 11:20:00"
  }
}
```

说明：

- 1.0 可只展示状态。
- 2.0 实现完整双方确认闭环时启用该接口。

### 6.3 确认阶段动作

```http
POST /api/stage-actions/{actionId}/confirm
```

权限：动作确认人

请求体：

```json
{
  "responseNote": "确认进入下一阶段。"
}
```

返回：阶段动作详情。

### 6.4 驳回阶段动作

```http
POST /api/stage-actions/{actionId}/reject
```

权限：动作确认人

请求体：

```json
{
  "responseNote": "需要补充竞品参考。"
}
```

返回：阶段动作详情。

### 6.5 阶段动作列表

```http
GET /api/projects/{projectId}/stage-actions
```

权限：项目关联客户 / 项目负责设计师

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| stageCode | string | 否 | 阶段编码 |
| status | string | 否 | PENDING / CONFIRMED / REJECTED / CANCELLED |

返回：阶段动作列表。

## 7. 会话与消息接口

### 7.1 当前用户会话列表

```http
GET /api/conversations
```

权限：客户 / 设计师

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "projectId": 1,
      "projectName": "山野咖啡品牌升级项目",
      "customerId": 1,
      "customerName": "演示客户",
      "designerId": 2,
      "designerName": "演示设计师",
      "lastMessage": "资料调研报告已提交，请客户确认。",
      "lastMessageAt": "2026-07-20 11:20:00",
      "unreadCount": 1
    }
  ]
}
```

### 7.2 会话消息列表

```http
GET /api/conversations/{conversationId}/messages
```

权限：会话参与人

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| beforeMessageId | long | 否 | 向上翻页锚点 |
| size | integer | 否 | 每次加载条数，默认 20 |

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 6,
        "conversationId": 1,
        "senderId": 2,
        "senderRole": "DESIGNER",
        "senderName": "演示设计师",
        "messageType": "TEXT",
        "content": "资料调研报告已提交，请客户确认。",
        "files": [],
        "createdAt": "2026-07-20 11:20:00"
      }
    ],
    "hasMore": false
  }
}
```

### 7.3 发送消息

```http
POST /api/conversations/{conversationId}/messages
```

权限：会话参与人

请求体：

```json
{
  "messageType": "TEXT",
  "content": "您好，我想确认一下设计风格。",
  "fileIds": [],
  "clientMessageId": "web-uuid-001"
}
```

返回：消息详情。

说明：

- 1.0 必须支持 `TEXT`。
- 2.0 支持 `IMAGE`、`FILE`、`EMOJI`。

### 7.4 标记会话已读

```http
POST /api/conversations/{conversationId}/read
```

权限：会话参与人

请求体：

```json
{
  "lastReadMessageId": 12
}
```

返回：已读状态详情。

说明：

- 1.0 可不在前端展示。
- 3.0 启用已读/未读时使用。

### 7.5 删除消息

```http
DELETE /api/messages/{messageId}
```

权限：消息发送人

返回：`Result<Boolean>`

说明：

- 建议软删除，设置 `isDeleted = 1`。

## 8. 文件接口

### 8.1 上传文件

```http
POST /api/files
Content-Type: multipart/form-data
```

权限：登录用户

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | 上传文件 |

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "originalName": "资料调研报告.pdf",
    "url": "https://example.com/files/report.pdf",
    "mimeType": "application/pdf",
    "fileSize": 2483200,
    "status": "ACTIVE"
  }
}
```

### 8.2 文件详情

```http
GET /api/files/{fileId}
```

权限：文件关联项目参与人 / 上传人

返回：文件详情。

### 8.3 下载文件

```http
GET /api/files/{fileId}/download
```

权限：文件关联项目参与人 / 上传人

返回：文件流。

### 8.4 删除文件

```http
DELETE /api/files/{fileId}
```

权限：上传人 / 负责设计师

返回：`Result<Boolean>`

## 9. 项目文件归档接口

### 9.1 项目文件列表

```http
GET /api/projects/{projectId}/files
```

权限：项目关联客户 / 项目负责设计师

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| stageCode | string | 否 | 阶段编码 |
| fileRole | string | 否 | 文件用途 |

返回：项目文件列表。

### 9.2 归档项目文件

```http
POST /api/projects/{projectId}/files
```

权限：项目负责设计师

请求体：

```json
{
  "fileId": 1,
  "projectStageId": 3,
  "stageCode": "RESEARCH_REPORT",
  "fileRole": "REPORT",
  "description": "山野咖啡资料调研报告。"
}
```

返回：项目文件详情。

### 9.3 删除项目文件归档

```http
DELETE /api/project-files/{projectFileId}
```

权限：项目负责设计师

返回：`Result<Boolean>`

## 10. 操作日志接口

### 10.1 项目操作日志

```http
GET /api/projects/{projectId}/operation-logs
```

权限：项目负责设计师

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| bizType | string | 否 | PROJECT / STAGE / MESSAGE / FILE |
| action | string | 否 | 操作动作 |
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页条数 |

返回：分页日志。

## 11. Swagger / YApi 生成说明

### 11.1 Swagger/OpenAPI

后端实现时建议引入 `springdoc-openapi-starter-webmvc-ui`，启动后访问：

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

Controller 必须使用：

- `@Tag`
- `@Operation`
- `@Parameter`
- `@RequestBody`
- `@Schema`

### 11.2 YApi

可将 `docs/openapi_1.0.yaml` 导入 YApi：

1. 进入 YApi 项目。
2. 选择“数据管理”。
3. 选择“导入数据”。
4. 数据格式选择 OpenAPI/Swagger。
5. 上传 `docs/openapi_1.0.yaml`。

## 12. 1.0 必须实现接口

1.0 必须实现：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `PUT /api/users/me`
- `GET /api/portfolio-cases`
- `GET /api/portfolio-cases/{id}`
- `GET /api/projects`
- `GET /api/projects/{id}`
- `GET /api/projects/{projectId}/stages`
- `GET /api/conversations`
- `GET /api/conversations/{conversationId}/messages`
- `POST /api/conversations/{conversationId}/messages`

1.0 可实现但前端不强依赖：

- 作品案例增删改。
- 项目增删改。
- 文件上传下载。
- 项目文件归档。
- 阶段动作接口。
- 已读接口。
- 操作日志接口。

