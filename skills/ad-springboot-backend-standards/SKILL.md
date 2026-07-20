---
name: ad-springboot-backend-standards
description: 广告设计公司客户对接平台后端专属开发规范。Use when developing, generating, refactoring, or reviewing code under advertisement_Design_back for Spring Boot 3.2, Java 17, MyBatis-Plus 3.5, MySQL 8.0, Lombok, MapStruct, Swagger/OpenAPI, Result<T>, BCrypt password handling, Service-layer business logic, DTO/VO boundaries, and Java Config.
---

# 广告平台后端开发规范

本 skill 仅适用于本项目 `advertisement_Design_back` 后端代码。开发、重构或审查后端代码时，必须遵守以下约束。

## 技术栈

- 使用 Java 17。
- 使用 Spring Boot 3.2。
- 使用 MyBatis-Plus 3.5。
- 使用 MySQL 8.0。
- 使用 Lombok 简化实体、DTO、VO 代码。
- 使用 MapStruct 处理 Entity、DTO、VO 之间的对象转换。
- 使用 Swagger/OpenAPI 注解描述 Controller 和接口方法。

## 分层规则

- Controller 只负责接收请求、基础参数校验、调用 Service、返回结果。
- 禁止在 Controller 中编写业务逻辑、数据库查询逻辑、状态流转逻辑或密码处理逻辑。
- Service 负责业务规则、事务边界、权限校验、状态流转、Mapper 协调。
- Mapper 使用 MyBatis-Plus `BaseMapper<T>`。
- 优先使用 MyBatis-Plus Wrapper、ServiceImpl 和 Java Config。
- 不使用过时 XML 配置；只有 SQL 极复杂且确有必要时，才允许补充 Mapper XML，并在技术文档中说明原因。

## 返回与对象边界

- Controller 必须统一返回 `Result<T>`。
- Controller 不得直接返回 Entity。
- 请求入参使用 Request DTO。
- 响应出参使用 Response VO。
- Entity 只用于数据库映射和 Service/Mapper 内部流转。
- Entity 到 VO、DTO 到 Entity 的常规转换优先使用 MapStruct。

## 实体与字段规范

- 所有实体类必须使用 Lombok `@Data` 和 `@Builder`。
- 实体类需要无参构造时，补充 `@NoArgsConstructor`。
- 使用 `@Builder` 且也需要全参构造时，补充 `@AllArgsConstructor`。
- 数据库表字段使用下划线命名，例如 `user_name`、`created_at`。
- Java 字段使用驼峰命名，例如 `userName`、`createdAt`。
- 使用 MyBatis-Plus 注解明确主键和表名，例如 `@TableName`、`@TableId`。

## API 规范

- 所有 Controller 类必须添加 Swagger/OpenAPI 类级注解。
- 所有 API 方法必须添加 Swagger/OpenAPI 方法级注解。
- 接口路径统一以 `/api` 开头。
- 认证接口放在 `/api/auth`。
- 作品案例接口放在 `/api/portfolio-cases`。
- 项目接口放在 `/api/projects`。
- 会话接口放在 `/api/conversations`。
- 阶段接口放在项目或会话上下文下，避免脱离项目单独修改阶段状态。

## 安全规范

- 密码必须使用 BCrypt 加密存储。
- 禁止明文保存密码。
- 禁止在日志、异常信息、接口响应中输出密码、密码哈希、token 密钥。
- 登录后使用 JWT 或同等 token 机制识别用户。
- 后端接口必须做角色和数据权限校验，不能只依赖前端路由守卫。
- 客户只能访问自己的项目、会话、消息和文件。
- 设计师只能访问自己负责的项目、会话、消息和文件。

## 异常与校验

- 使用统一异常处理，不在 Controller 中重复 try/catch 业务异常。
- 参数校验优先使用 Jakarta Validation 注解。
- 业务错误通过统一错误码和 `Result<T>` 返回。
- 未登录返回 401 语义。
- 无权限返回 403 语义。
- 资源不存在返回 404 语义。

## 生成代码要求

- 生成 Java 代码时自动补全必要 import。
- 不生成无法编译的占位 import。
- 不省略包名、注解、泛型类型和访问修饰符。
- 新增接口时同步补齐 DTO/VO、Service 方法、必要 Mapper 调用和 Swagger/OpenAPI 注解。
- 涉及密码、登录、注册时必须使用 BCrypt。
- 涉及 Controller 返回时必须使用 `Result<T>`。

