# 阿里云 OSS 双 Bucket 接入与首批图片上线指南

## 一、当前实现结论

本项目已经预留“公私双 Bucket”存储方案，前端不会直接调用阿里云 OSS，所有上传均先进入后端 Controller，再由 `common.storage` 调用 OSS SDK。

公开 Bucket 用于作品案例封面和详情图片；私有 Bucket 用于项目合同、需求资料、报告、草稿、定稿、交付件、会话图片和会话附件。MySQL 的 `file_asset` 表保存上传人、存储提供商、Bucket、Object Key、公开 URL、MIME、大小、SHA-256 和状态，真实文件字节保存在 OSS。

当前公开图片接口为 `POST /api/portfolio-cases/images`。`cover=true` 上传封面，`cover=false` 上传详情图。项目文件接口为 `POST /api/projects/{projectId}/file-assets?fileRole=...`，会话附件接口为 `POST /api/conversations/{conversationId}/file-assets?image=...`。这些接口均需要 JWT，并在后端执行角色或业务参与者权限校验。

## 二、首批需要准备多少图片

建议先准备 31 张：1 张首页主视觉、5 张案例封面、25 张案例详情图。每个案例对应 1 张封面和 5 张详情图，五个案例分别对应品牌视觉、营销传播、包装设计、空间设计和数字体验服务。

当前案例上传接口可以直接接入其中 30 张，即 5 张案例封面和 25 张详情图。首页主视觉目前没有独立的后台业务记录和上传接口，不要冒充案例图片入库；可以先保留原图，等首页站点配置接口补充后再上传并绑定。如果暂时必须上线，可将它作为前端部署资产处理，但这不属于本次 OSS 业务上传链路。

图片建议统一使用 JPG 或 WebP；需要透明背景时使用 PNG。封面建议至少 1600×1100，详情图建议宽度至少 1800 px。单文件应控制在 8 MB 内，虽然当前后端上限默认为 20 MB。后端图片场景只接受 JPG/JPEG、PNG、GIF 和 WebP，并同时校验扩展名与声明的 MIME 类型；不要上传 SVG、PDF 或仅修改扩展名的文件。

建议按以下方式整理本地文件名：`brand-cover.webp`、`brand-detail-01.webp` 至 `brand-detail-05.webp`，其余服务使用 `marketing`、`packaging`、`spatial`、`digital` 前缀。OSS 中的最终 Object Key 由后端自动生成，不需要在阿里云控制台预建目录。

## 三、创建两个 Bucket

登录阿里云 OSS 控制台，在与后端服务器相同或邻近的地域创建两个 Bucket。两个 Bucket 必须处于同一地域，名称必须全局唯一，创建后地域和名称不能随意变更。

建议命名为：

```text
<公司缩写>-advertisement-public
<公司缩写>-advertisement-private
```

公开 Bucket 选择标准存储、同城冗余是否开启可按预算决定，读写权限选择“公共读”。只允许匿名读取，不允许匿名写入。私有 Bucket 同样选择标准存储，读写权限必须选择“私有”。不要为私有 Bucket 开启公共读，不要把合同、客户资料、项目稿件或会话附件放入公开 Bucket。

如果阿里云账号启用了“阻止公共访问”，需要仅对公开 Bucket 做经过确认的例外配置；私有 Bucket继续保持阻止公共访问。公开 Bucket 最好只保存本项目的公开案例资产，避免混放其他数据。

本项目不采用浏览器直传 OSS，因此不需要为 OSS 配置跨域上传 CORS。浏览器只访问公开图片 URL；上传请求发往后端 API。

## 四、创建专用 RAM 用户和最小权限

不要使用阿里云主账号 AccessKey。进入 RAM 控制台，创建仅供此后端使用的 RAM 用户，例如 `advertisement-backend-oss`，只开启 OpenAPI 访问，生成 AccessKey ID 和 AccessKey Secret。Secret 只会显示一次，应保存到部署平台的密钥管理或环境变量中，不能写入 Git、前端代码、截图、聊天记录或日志。

创建自定义权限策略，将下面两个 Bucket 名替换为真实名称：

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "oss:PutObject",
        "oss:GetObject",
        "oss:DeleteObject"
      ],
      "Resource": [
        "acs:oss:*:*:<public-bucket>/*",
        "acs:oss:*:*:<private-bucket>/*"
      ]
    }
  ]
}
```

将该策略授权给专用 RAM 用户。当前实现上传、后端下载和补偿删除分别需要 `PutObject`、`GetObject` 和 `DeleteObject`，不需要授予 Bucket 管理、权限修改或全账号 OSS 管理权限。若以后增加对象列表、分片上传或生命周期管理，再按实际 API 单独增加权限。

## 五、配置公开访问域名

生产环境建议为公开 Bucket 绑定单独域名，例如 `assets.example.com`，并配置 HTTPS 证书。DNS CNAME 按 OSS 控制台给出的目标地址设置。`OSS_PUBLIC_DOMAIN` 必须填写完整的 HTTPS 根地址且末尾不要带 `/`，例如：

```text
https://assets.example.com
```

如果域名和证书尚未完成，可先使用公开 Bucket 的 HTTPS 访问域名进行联调，但正式上线前应切换到自有域名。不要把私有 Bucket 的域名配置为公开域名。

## 六、设置后端环境变量

在后端运行环境配置以下变量，真实值不要写入 `application.yml` 或 `application-local.yml`：

```bash
STORAGE_PROVIDER=OSS
OSS_ENABLED=true
OSS_ENDPOINT=https://oss-cn-<region>.aliyuncs.com
OSS_REGION=oss-cn-<region>
OSS_ACCESS_KEY_ID=<RAM用户AccessKeyId>
OSS_ACCESS_KEY_SECRET=<RAM用户AccessKeySecret>
OSS_PUBLIC_BUCKET=<公开Bucket名称>
OSS_PRIVATE_BUCKET=<私有Bucket名称>
OSS_PUBLIC_DOMAIN=https://assets.example.com
OSS_SIGNED_URL_EXPIRATION=15m
STORAGE_MAX_FILE_SIZE=20971520
JWT_SECRET=<至少32字节的高强度随机密钥>
DB_USERNAME=<数据库用户>
DB_PASSWORD=<数据库密码>
MAIL_USERNAME=<邮件账号>
MAIL_PASSWORD=<邮件授权码>
MAIL_FROM=<发件地址>
```

`OSS_ENDPOINT` 使用部署服务器可访问的 Endpoint。后端与 OSS 同地域且部署网络支持内网访问时，可评估使用内网 Endpoint 来减少公网流量；本地开发通常使用公网 Endpoint。当前 `OSS_REGION` 和 `OSS_SIGNED_URL_EXPIRATION` 已预留，但私有文件目前采用后端鉴权后流式下载，并未生成临时签名 URL。

启动前检查 `application-local.yml`：其中如仍有历史数据库密码、邮件授权码或 JWT 密钥，应改为环境变量并立即轮换任何曾经暴露的凭据。

## 七、启动和基础连通性验证

必须使用 Java 17 和 Maven。当前检查环境只有 Java 11 且没有 Maven，因此应在你的正式开发机或 CI 中执行：

```bash
cd advertisement_Design_back
mvn clean test
mvn spring-boot:run
```

OSS 配置缺少 Endpoint、AccessKey、任一 Bucket 或公开域名时，启用 OSS 后后端会启动失败，这是预期的快速失败行为。后端正常启动后打开：

```text
http://localhost:8080/swagger-ui.html
```

先用设计师账号登录，取得 JWT。不要在共享终端历史或截图中暴露生产 token。

## 八、按后端接口上传 30 张案例图片

每张图片都通过后端上传，不要在前端保存 AccessKey，也不要使用浏览器直传 OSS。

封面上传示例：

```bash
curl -X POST \
  'http://localhost:8080/api/portfolio-cases/images?cover=true' \
  -H 'Authorization: Bearer <designer-jwt>' \
  -F 'file=@brand-cover.webp;type=image/webp'
```

详情图上传示例：

```bash
curl -X POST \
  'http://localhost:8080/api/portfolio-cases/images?cover=false' \
  -H 'Authorization: Bearer <designer-jwt>' \
  -F 'file=@brand-detail-01.webp;type=image/webp'
```

成功响应中的关键字段包括 `id`、`storageProvider`、`bucketName`、`objectKey`、`url`、`mimeType`、`fileSize` 和 `fileHash`。应确认：

```text
storageProvider = OSS
bucketName = 公开 Bucket
objectKey 的封面前缀 = portfolio/covers/
objectKey 的详情前缀 = portfolio/details/
url 以 OSS_PUBLIC_DOMAIN 开头
status = ACTIVE
```

按“一个案例的封面，然后该案例的五张详情图”的顺序上传，立即把返回的 URL 记录到表格中，避免混淆。五个案例共执行 30 次上传。

## 九、创建或更新五个案例记录

上传图片只会写入 `file_asset`，不会自动创建 `portfolio_case`。每个案例上传完成后，使用返回的公开 `url` 调用案例新增接口：

```bash
curl -X POST \
  'http://localhost:8080/api/portfolio-cases' \
  -H 'Authorization: Bearer <designer-jwt>' \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "案例标题",
    "category": "BRAND",
    "industry": "客户行业",
    "style": "设计风格",
    "serviceType": "品牌设计",
    "coverUrl": "https://assets.example.com/portfolio/covers/...",
    "imageUrls": [
      "https://assets.example.com/portfolio/details/...",
      "https://assets.example.com/portfolio/details/...",
      "https://assets.example.com/portfolio/details/...",
      "https://assets.example.com/portfolio/details/...",
      "https://assets.example.com/portfolio/details/..."
    ],
    "description": "案例设计说明",
    "sortOrder": 1,
    "featured": true,
    "status": "PUBLISHED"
  }'
```

五个案例都应设置 `featured=true`，`sortOrder` 依次为 1 至 5。`category` 当前可使用 `BRAND`、`DIGITAL` 或 `OFFLINE`；`serviceType` 应与前端五个服务分类的名称保持一致。创建后调用 `GET /api/portfolio-cases?featured=true&page=1&size=5`，确认首页能得到五条记录，再分别调用 `GET /api/portfolio-cases/{id}` 检查封面和五张详情图。

不要只在数据库中手工填写 URL而跳过上传接口，否则 `file_asset` 不会保存元数据与哈希，也无法使用统一删除和审计逻辑。

## 十、验证私有 Bucket

使用已分配项目的设计师账号上传一份无敏感内容的测试 PDF：

```bash
curl -X POST \
  'http://localhost:8080/api/projects/<projectId>/file-assets?fileRole=REPORT' \
  -H 'Authorization: Bearer <designer-jwt>' \
  -F 'file=@oss-private-test.pdf;type=application/pdf'
```

确认响应中 `bucketName` 为私有 Bucket，`objectKey` 前缀为 `projects/reports/`，`url` 为空。随后按现有项目归档接口将返回的文件 ID 归档，再通过 `GET /api/files/{fileId}/download` 验证：项目参与者可下载，无关客户或设计师得到 403，未登录请求得到 401。最后确认直接访问 OSS Object URL不能匿名读取。

会话测试可使用 `POST /api/conversations/{conversationId}/file-assets?image=true` 上传 JPG/WebP，或将 `image=false` 上传普通附件。只有会话参与者可以上传。

## 十一、成本、生命周期与安全建议

公开作品图片建议使用标准存储，并在前端启用 WebP、合理压缩和懒加载。可以在 CDN 或 OSS 域名层配置长缓存，但替换图片时应使用新的 Object Key，而不是覆盖旧对象，以避免缓存不一致。

私有 Bucket 不建议立即对合同和最终交付件设置自动删除。可先仅针对明确的临时草稿目录制定生命周期策略，例如在业务确认后将 `projects/drafts/` 中长期未使用对象转低频存储；任何自动删除规则上线前必须与数据库记录和公司留档周期一致。会话附件、合同和最终交付件应根据合同、财务和合规要求确定保留期限。

开启 OSS 服务端加密和访问日志时，要评估额外费用。设置费用预警和资源包使用提醒。定期轮换 RAM AccessKey；应用退出时 OSS Client 会正常关闭。不要记录 AccessKey、JWT 密钥、密码哈希或 token。

## 十二、上线验收清单

上线前应完成以下验收：后端使用 Java 17；Maven 全量测试通过；两个前端的 `vue-tsc -b` 通过；公开图片可以匿名读取但不能匿名写入；私有对象不能匿名读取；五个首页案例各有一张封面和五张详情图；30 次图片上传均在 `file_asset` 中有 OSS 元数据；五个 `portfolio_case` 均绑定真实 URL；项目和会话越权测试返回 403；JWT、数据库、邮件和 OSS 密钥均来自部署环境；仓库和前端构建产物中不存在真实凭据。

当前本地静态验证已经确认两个 Vue 工程的 TypeScript 检查通过，`git diff --check` 通过。Java 单元测试和编译尚未在当前环境执行，因为当前只有 OpenJDK 11.0.31，且 Maven 与 Maven Wrapper 均不可用；部署前必须在 Java 17 + Maven 环境补跑。
