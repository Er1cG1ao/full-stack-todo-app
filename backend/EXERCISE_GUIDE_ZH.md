# Todo Backend Workshop 练习整理

这个 workshop 用 9 个步骤、19 个练习点，把一个 Spring Boot 后端从“能启动”补全为可供 Angular 前端调用的 Todo CRUD API。代码已全部完成，下面保留每个练习的目标和必须理解的原因。

| 步骤 | 练习 | 位置 | 核心知识 |
|---|---|---|---|
| Step 1 | ① 固定问候接口 ② 路径变量接口 | `HelloWorldController` | `@GetMapping`、`@PathVariable`、JSON record |
| Step 2 | ① 全局 CORS | `WebConfig` | 前后端跨端口、preflight、允许的 HTTP methods/headers |
| Step 3 | ① 无参构造器 ② `done` getter/setter | `Todo` | Jackson 反序列化、JavaBean 布尔命名规则 |
| Step 4 | ① 按用户过滤 ② 服务端校验 | `TodoInMemoryService` | 数据隔离、不信任客户端验证 |
| Step 5 | ① 查单条 ② 构造器注入 ③ 查列表 ④ 单条 GET | service + resource | `Optional.orElseThrow`、DI、REST 资源映射 |
| Step 6 | ① 删除业务 ② DELETE 接口 | service + resource | 所有权检查、`204 No Content` |
| Step 7 | ① 更新业务 ② PUT 接口 | service + resource | path id 优先、原地更新、PUT 幂等性 |
| Step 8 | ① 创建业务 ② POST 接口 | service + resource | 后端生成 id、path username 优先、`201` + `Location` |
| Step 9 | ① Not Found ② Invalid Input | `GlobalExceptionHandler` | 业务异常与 HTTP 分层、统一错误 JSON |

## 建议学习顺序

1. 先读 `TodoService`，理解 controller 只依赖接口，而不依赖内存实现。
2. 按 Step 1–4 理解 HTTP 入口、CORS、JSON 模型和业务校验。
3. 用 Step 5–8 把 CRUD 的 controller 与 service 一一对照。
4. 最后看 Step 9：service 只抛业务异常，全局 handler 负责翻译成 400/404。

## 验证方式

```bash
./mvnw test
./mvnw spring-boot:run
```

启动后可以用 `api.http` 逐条发送请求，或打开 `http://localhost:8080/swagger-ui.html` 交互式测试。特别检查：

- 访问别人的 Todo 得到 404，不会泄露数据。
- POST 忽略 body 里的 id/username，返回 201、`Location` 和创建后对象。
- PUT 以 path id 为准，不改变记录的所有者和列表位置。
- DELETE 成功返回 204；重复删除返回带 `message` 的 404。
- 描述少于 5 个字符或缺少日期时，返回带 `message` 的 400。
