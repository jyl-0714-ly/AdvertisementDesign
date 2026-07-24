# 广告平台后端开发规范

本 Skill 仅适用于 `advertisement_Design_back`。后端采用模块化单体：仍是一个 Spring Boot 应用、一个部署单元和一个数据库，不拆微服务。

## 技术栈

- Java 17、Spring Boot 3.2、MyBatis-Plus 3.5、MySQL 8.0。
- 使用 Lombok、MapStruct、Swagger/OpenAPI。
- 优先使用 Java Config，不使用过时 XML 配置。
- 仅在 SQL 确实复杂时使用 Mapper XML，并说明原因。

## 顶层业务模块

禁止继续新增全局 `controller/`、`service/`、`mapper/`、`domain/`、`api/` 技术分层。新代码和已迁移代码必须归入以下业务模块：

```text
com.advertisementdesign.back
├── auth
├── identity
├── consultation
├── project
├── communication
├── portfolio
└── common
```

### 模块职责

- `auth`：登录、注册、邮箱验证码、JWT、Spring Security 认证流程。
- `identity`：用户资料、客户、设计师资料、角色、状态及后续 RBAC。
- `consultation`：AI 咨询、人工咨询、需求单、需求分析、设计师匹配。
- `project`：项目、项目阶段、阶段操作、阶段确认、状态机、项目交付文件。
- `communication`：会话、消息、已读状态、聊天附件及后续 WebSocket。
- `portfolio`：作品案例、分类、筛选、发布和展示。
- `common`：统一返回、异常、用户上下文、通用配置、审计和底层文件存储。

不要提前创建没有实际代码和数据生命周期的空模块。合同和支付形成真实业务后，再新增 `commercial`，不要现在创建空的 `contract`、`payment` 包。

## 模块内部结构

按实际需要创建目录，不强制生成空包：

```text
project
├── controller
├── service
├── mapper
├── entity
├── dto
├── vo
├── enums
├── converter
└── repository       # 仅在需要隔离持久化实现时使用
```

- Controller、Service、Mapper、Entity、DTO、VO、枚举必须放在所属业务模块内。
- 小项目不强制每个 Service 都创建接口和 `Impl`；只有多实现、测试替身或稳定边界确有价值时才增加接口。
- 不创建没有实际复杂度收益的 Facade、Manager、Domain Service 或多层适配器。
- 全局 `ApiAssembler` 应逐步拆为模块内 Converter/Assembler，模块不得依赖全局业务聚合器。



## 模块依赖

允许的主要依赖方向：

```text
auth          -> identity, common
consultation  -> identity, project, common
project       -> identity, common
communication -> identity, project, common
portfolio     -> common
common        -> 不依赖任何业务模块
```

- 模块之间不得直接调用对方 Mapper。
- 模块之间不得直接读写对方 Entity 或数据库表。
- 跨模块调用通过对方公开 Service/Facade 及 DTO/VO 完成；小项目不要求为每个调用机械创建 Facade。
- 只传递 ID、命令 DTO、查询 DTO 或公开 VO，不把 Entity 作为模块间契约。
- 禁止循环依赖。发现双向依赖时，重新确认业务所有权，或使用应用事件解除同步反向依赖。
- `common` 不得包含项目、消息、咨询等业务规则，也不得成为随意堆放代码的目录。



## Controller、Service 与 Mapper

- Controller 只负责接收请求、参数校验、调用本模块 Service、返回 `Result<T>`。
- 禁止在 Controller 中编写业务逻辑、SQL、状态流转、权限规则或密码处理。
- Service 负责业务规则、事务边界、数据权限、状态流转和本模块 Mapper 协调。
- 事务优先定义在 Service 的业务用例方法上。
- Mapper 放在所属模块，使用 MyBatis-Plus `BaseMapper<T>`。
- 优先使用 MyBatis-Plus Wrapper；不要为了复用查询而跨模块共享 Mapper。



## DemoDataStore 迁移规则

`DemoDataStore` 是临时内存实现，不是目标架构。

- 禁止为新功能继续向全局 `DemoDataStore` 增加跨领域集合和业务方法。
- 禁止将整个 `DemoDataStore` 移入 `common` 后继续使用；这不属于模块化。
- 按 `auth/identity -> portfolio -> project -> communication -> common.storage -> consultation` 顺序逐模块迁移。
- 迁移模块时，先建立模块边界和模块内 Mapper/Repository，再将该模块的数据访问替换为 MySQL。
- 过渡期允许模块内的 InMemory Repository 委托旧 Store，但 Service 不应继续直接依赖全局 Store。
- 一个模块完成数据库迁移和回归测试后，删除 `DemoDataStore` 中对应集合、序列和方法。
- 不在同一次改动中同时进行全量包迁移、全量 SQL 替换和大规模业务功能开发。



## 文件归属

- `common.storage` 管理文件资产元数据、物理存储、上传和下载基础能力。
- `communication` 管理聊天附件与消息的业务关联和访问权限。
- `project` 管理设计稿、报告、合同材料、交付物与项目阶段的业务关联和访问权限。
- 底层可引用同一个文件资产 ID，但权限必须由拥有该业务关系的模块校验。
- 不把聊天附件和项目交付文件混为同一种业务对象。



## DTO、VO 与实体

- Controller 必须统一返回 `Result<T>`，不得直接返回 Entity。
- 请求使用 Request DTO，响应使用 Response VO。
- Entity 只用于所属模块的数据映射和内部持久化流转。
- 常规 Entity、DTO、VO 转换优先使用模块内 MapStruct Converter。
- 所有实体类使用 Lombok `@Data` 和 `@Builder`。
- 需要无参或全参构造时补充 `@NoArgsConstructor`、`@AllArgsConstructor`。
- 数据库字段使用下划线命名，Java 字段使用驼峰命名。
- 使用 `@TableName`、`@TableId` 等 MyBatis-Plus 注解明确映射。



## API 规范

- 所有 Controller 类和 API 方法添加 Swagger/OpenAPI 注解。
- 接口统一以 `/api` 开头。
- 包迁移不得随意修改已有 API 路径、请求格式、响应格式和状态语义。
- 认证接口使用 `/api/auth`，作品接口使用 `/api/portfolio-cases`，项目接口使用 `/api/projects`，会话接口使用 `/api/conversations`。
- 阶段操作必须处于项目上下文中，不提供脱离项目任意修改阶段状态的接口。



## 安全规范

- 密码必须使用 BCrypt，禁止明文保存。
- 禁止在日志、异常和响应中输出密码、密码哈希、验证码、JWT 密钥或完整 Token。
- 后端必须执行角色与数据权限校验，不能只依赖前端。
- 客户只能访问自己的项目、会话、消息和文件。
- 设计师只能访问自己负责的项目、会话、消息和文件。
- RBAC 放在 `identity.permission`；JWT 和认证流程放在 `auth`。



## 后续能力归属

- 项目状态机放在 `project.workflow`，阶段状态只能通过明确动作迁移。
- WebSocket 放在 `communication.websocket`，它是通信适配方式，不是独立业务模块。
- RBAC 放在 `identity.permission`。
- 简单 AI 咨询放在 `consultation.ai`；当 Agent 拥有独立知识库、记忆、工具调用和生命周期时，再拆出 `assistant` 模块。



## 异常、测试与交付

- 使用全局异常处理，不在 Controller 重复捕获业务异常。
- 参数校验使用 Jakarta Validation。
- 未认证、无权限、资源不存在分别保持 401、403、404 语义。
- 测试包按业务模块镜像组织，不继续新增全局 `service` 测试包。
- 每迁移一个模块，至少执行编译、模块 Service 测试和相关 API 回归。
- 重构时保持数据库表、API 契约和业务行为不变；结构迁移与功能调整分开验证。



## 生成代码要求

- 自动补全必要 Import，不生成占位 Import。
- 不省略包名、注解、泛型和访问修饰符。
- 新增接口时同步补齐 DTO/VO、Service、Mapper/Repository、转换器、Swagger 注解和必要测试。
- 新代码必须直接进入正确业务模块，不得先放入旧的全局技术分层等待以后整理。

