# 图书管理系统 — 需求与易错点分析文档

> 基于项目源码全方位分析生成，重点标注易错点与风险项。

---

## 一、项目概览

| 维度 | 说明 |
|------|------|
| 技术栈 | Spring Boot + MyBatis-Plus + MySQL + JWT + Vue 2 + Element UI |
| 架构 | 前后端分离，单SPA（管理员端 + 借阅者C端） |
| 核心模块 | 图书管理、人员管理、借阅/归还、逾期提醒、C端借阅者自助查询 |
| 认证方式 | JWT Token（管理员：用户名+密码，借阅者：手机号+密码） |

---

## 二、功能模块需求

### 2.1 管理员端（B端）

| 功能 | 接口 | 说明 |
|------|------|------|
| 登录 | `POST /api/auth/login` | 用户名+密码，返回JWT（含`userType=ADMIN`） |
| 当前用户 | `GET /api/auth/current-user` | 获取当前登录管理员信息 |
| 退出登录 | `POST /api/auth/logout` | 清除SecurityContext |
| 图书CRUD | `/api/book/**` | 增删改查图书，含书名/作者/ISBN/出版社/分类/总数/可借数 |
| 人员CRUD | `/api/user/**` | 增删改查借阅者，含姓名/电话/邮箱/地址/密码 |
| 借阅操作 | `POST /api/borrow/borrow` | 选择借阅者+图书，设定借阅天数 |
| 归还操作 | `POST /api/borrow/return/{id}` | 归还图书，恢复可借数 |
| 借阅记录 | `GET /api/borrow/list` | 查看全部借阅记录（含关联用户和图书信息） |
| 逾期提醒 | `GET /api/borrow/reminders` | 即将到期(≤3天) + 已逾期记录 |
| 仪表盘 | - | 图书总数/用户数/借阅中数量 + 逾期提醒表格 |

### 2.2 借阅者端（C端）

| 功能 | 接口 | 说明 |
|------|------|------|
| 登录 | `POST /api/auth/borrower/login` | 手机号+密码，返回JWT（含`userType=BORROWER`+`userId`） |
| 我的借阅 | `GET /api/borrower/my-records` | 从JWT提取userId，仅返回自己的借阅记录 |

### 2.3 定时任务

| 任务 | 触发时间 | 说明 |
|------|---------|------|
| 逾期标记 | 每天凌晨2:00 | 将`status=0`且`due_date < NOW()`的记录标记为status=2 |

---

## 三、数据库表结构

### 3.1 tb_book（图书表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| name | VARCHAR(100) NOT NULL | 书名 |
| author | VARCHAR(50) NOT NULL | 作者 |
| isbn | VARCHAR(20) | ISBN号 |
| publisher | VARCHAR(100) | 出版社 |
| category | VARCHAR(50) | 分类 |
| total_count | INT NOT NULL | 总数量 |
| available_count | INT NOT NULL | 可借数量 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

> **易错点**：`isbn`无唯一约束；`available_count`可被客户端传入任意值。

### 3.2 tb_user（借阅者表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| name | VARCHAR(50) NOT NULL | 姓名 |
| phone | VARCHAR(20) | 电话（C端登录账号） |
| email | VARCHAR(100) | 邮箱 |
| address | VARCHAR(200) | 地址 |
| password | VARCHAR(200) | BCrypt加密密码 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

> **易错点**：`phone`无唯一约束，可能导致手机号重复（登录时只能匹配到第一条）。

### 3.3 tb_system_user（系统管理员表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| username | VARCHAR(50) UNIQUE NOT NULL | 登录用户名 |
| password | VARCHAR(200) NOT NULL | BCrypt加密密码 |
| real_name | VARCHAR(50) | 真实姓名 |
| role | VARCHAR(20) DEFAULT 'ADMIN' | 角色 |
| status | TINYINT DEFAULT 1 | 1-启用 0-禁用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 3.4 tb_borrow_record（借阅记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| user_id | BIGINT NOT NULL | 借阅人ID |
| book_id | BIGINT NOT NULL | 图书ID |
| borrow_date | DATETIME NOT NULL | 借阅日期 |
| due_date | DATETIME NOT NULL | 应还日期 |
| return_date | DATETIME | 实际归还日期 |
| status | TINYINT DEFAULT 0 | 0-借阅中 1-已归还 2-已逾期 |
| create_time | DATETIME | 创建时间 |
| idx_user_id | INDEX | user_id索引 |
| idx_book_id | INDEX | book_id索引 |
| idx_status | INDEX | status索引 |

> **易错点**：`user_id`和`book_id`无外键约束；`due_date`无索引，逾期查询全表扫描。

---

## 四、API响应契约

### 4.1 Auth类接口（使用Result&lt;T&gt;包装）

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误时：
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

### 4.2 业务类接口（使用Map包装，键名略有差异）

```json
{
  "code": 200,
  "data": [ ... ],
  "message": "操作成功"
}
```

> **易错点**：AuthController用`Result<T>`，其他Controller用`Map<String,Object>`手动构建，两种方式共存。`Result<T>`不含`success`布尔字段，前端通过`data.code === 200`判断，若后端改为HTTP状态码则前端判断会失效。

---

## 五、JWT Token结构

```
Header: { "alg": "HS256" }
Payload: {
  "sub": "admin" | "13800000000",
  "role": "ADMIN" | "BORROWER",
  "userId": null | 123,
  "userType": "ADMIN" | "BORROWER",
  "iat": 1717286400,
  "exp": 1717372800
}
```

Token有效期为24小时（`86400000ms`）。

> **易错点**：
> - Admin token的`sub`是`username`，Borrower token的`sub`是`phone`，语义不同。
> - 管理员token中`userId=null`，Borrower token中`userId`是实际的用户ID。
> - 后端虽在filter中提取了`ROLE_ADMIN`/`ROLE_BORROWER`，但SecurityConfig并未通过`.hasRole()`强制校验。

---

## 六、前端路由结构

| 路径 | 页面 | 访问要求 |
|------|------|---------|
| `/login` | 管理员登录页 | 公开，已登录则跳首页 |
| `/` | 仪表盘 | 管理员登录 |
| `/book` | 图书管理 | 管理员登录 |
| `/user` | 人员管理 | 管理员登录 |
| `/borrow-list` | 借阅记录 | 管理员登录 |
| `/borrow` | 借阅/归还 | 管理员登录 |
| `/borrower/login` | C端登录页 | 公开，C端已登录则跳我的借阅 |
| `/borrower/my-records` | 我的借阅记录 | 借阅者登录 |

权限隔离由前端路由守卫的`userType` localStorage值控制，后端未强制校验角色。

---

## 七、易错点详细分析

### 7.1 🔴 严重 — 并发竞争：借阅/归还可借数量非原子操作

**文件**：`service/impl/BorrowServiceImpl.java:borrowBook()` / `returnBook()`

**问题描述**：
借阅操作采用「先读后写」模式：

```java
Book book = bookMapper.selectById(bookId);     // 读取 availableCount
book.setAvailableCount(book.getAvailableCount() - 1);  // 内存计算
bookMapper.updateById(book);                    // 写回
```

两个并发借阅请求同时读到`availableCount=3`，各自减1后均写回2，实际剩余应为1。这会导致**超借**（借出超过库存）。

归还操作同样有**丢失归还**的风险：两个并发归还同时读到`availableCount=3`，各自加1后均写回4，实际应为5。

**建议修复**：
```sql
UPDATE tb_book SET available_count = available_count - 1
WHERE id = ? AND available_count > 0
```
检查受影响行数，若为0则返回"库存不足"。

---

### 7.2 🔴 严重 — 后端未强制角色授权，借阅者token可访问管理员API

**文件**：`config/SecurityConfig.java:authorizeRequests()`

**问题描述**：
```java
.antMatchers("/api/auth/**").permitAll()
.antMatchers("/api/**").authenticated()
```

只有`authenticated()`，没有`hasRole("ADMIN")`。任何人持有有效JWT（包括借阅者）都可以访问管理员接口，如：
- `POST /api/book/add` — 任意添加图书
- `DELETE /api/user/delete/5` — 删除用户
- `POST /api/borrow/borrow` — 以任意用户身份借书

当前仅在**前端**路由守卫中通过`meta.adminOnly`限制，但后端无保护。

**建议修复**：
```java
.antMatchers("/api/auth/**").permitAll()
.antMatchers("/api/borrower/**").hasRole("BORROWER")
.antMatchers("/api/**").hasRole("ADMIN")
```

---

### 7.3 🔴 严重 — 客户端可篡改`availableCount`和`totalCount`

**文件**：`controller/BookController.java:add()` + `service/impl/BookServiceImpl.java:addBook()`

**问题描述**：
`BookController`直接接受`Book`实体作为请求体，客户端可传入任意值：
```json
{ "totalCount": 5, "availableCount": 100 }
```
导致`availableCount > totalCount`，数据不一致。

**建议修复**：`addBook()`中始终将`availableCount`强制设为`totalCount`，不接受客户端传入的`availableCount`。

---

### 7.4 🔴 严重 — JWT签名密钥硬编码在源码中

**文件**：`resources/application.yml:jwt.secret`

```yaml
jwt:
  secret: library-jwt-secret-key-2024-very-long-secret
```

**问题描述**：密钥提交到Git仓库，任何有代码访问权限的人均可伪造任意用户的JWT。

**建议修复**：通过环境变量`${JWT_SECRET}`注入，或使用密钥管理服务。

---

### 7.5 🔴 严重 — CORS配置允许任意来源带凭证请求

**文件**：`config/SecurityConfig.java:corsConfigurationSource()`

```java
config.setAllowedOriginPatterns(Arrays.asList("*"));
config.setAllowedMethods(Arrays.asList("*"));
config.setAllowedHeaders(Arrays.asList("*"));
config.setAllowCredentials(true);
```

**问题描述**：`*` + `AllowCredentials(true)`意味着任意域可携带Cookie/Token发起认证请求。

---

### 7.6 🟠 高危 — 登出不终止JWT，Token可被复用

**文件**：`controller/AuthController.java:logout()`

```java
@PostMapping("/logout")
public Result<String> logout() {
    SecurityContextHolder.clearContext();  // 仅清当前线程上下文
    return Result.success("退出成功");
}
```

**问题描述**：JWT在有效期内（24小时）始终可用，登出后Token仍可请求所有API。没有黑名单/撤销机制。

---

### 7.7 🟠 高危 — 登录接口无限速，可被暴力破解

**文件**：`controller/AuthController.java:login()` + `controller/BorrowerAuthController.java:login()`

**问题描述**：无请求频率限制、无验证码、无账户锁定。攻击者可持续尝试密码组合。

---

### 7.8 🟠 高危 — 全局无服务端输入校验

**问题描述**：项目中所有Controller均未使用`@Valid`注解，DTO/Entity无`@NotNull`/`@Size`/`@Min`等校验注解，`pom.xml`未引入`spring-boot-starter-validation`。

示例后果：
- `borrowDays`可为负数，导致`dueDate < borrowDate`（借书时即"逾期"）
- `phone`/`email`无格式校验
- 字符串字段无限长
- `name`可为空但仍写入DB

---

### 7.9 🟠 高危 — 异常捕获过于宽泛，无日志记录

**文件**：所有Controller（`BookController`, `UserController`, `BorrowController`等）

**典型模式**：
```java
try {
    // ...
    result.put("code", 200);
} catch (Exception e) {
    result.put("code", 500);
    result.put("message", e.getMessage());
}
```

**问题**：
- 捕获所有异常（包括NPE等编程错误），全部转成HTTP 200 + code 500
- 无任何日志，异常原因彻底丢失
- `e.getMessage()`可能为null（如NPE），返回空消息给前端
- 直接暴露异常信息给客户端（信息泄露）

---

### 7.10 🟠 高危 — 借阅时不校验用户是否存在

**文件**：`service/impl/BorrowServiceImpl.java:borrowBook()`

**问题描述**：`record.setUserId(dto.getUserId())`直接使用客户端传来的userId，不做存在性校验。可对不存在的用户ID创建借阅记录。

由于`tb_borrow_record`的`user_id`无FK约束，DB也不会阻止。

---

### 7.11 🟡 中危 — `availableCount`字段在创建时可被客户端覆盖

**文件**：`service/impl/BookServiceImpl.java:addBook()`

```java
if (book.getAvailableCount() == null) {
    book.setAvailableCount(book.getTotalCount());
}
```

如果`availableCount`非null但`totalCount`为null，则`availableCount`被保留但无对应的`totalCount`。

---

### 7.12 🟡 中危 — 图书被删除后归还操作静默丢失库存

**文件**：`service/impl/BorrowServiceImpl.java:returnBook()`

```java
Book book = bookMapper.selectById(record.getBookId());
if (book != null) {
    book.setAvailableCount(book.getAvailableCount() + 1);
    bookMapper.updateById(book);
}
```

如果图书已被删除，`book == null`时归还操作跳过计数恢复，`availableCount`永久丢失。且无任何日志告警。

---

### 7.13 🟡 中危 — GET请求有副作用（REST反模式）

**文件**：`controller/BorrowController.java:reminders()`

```java
@GetMapping("/reminders")
public Map<String, Object> reminders() {
    reminderService.updateOverdueStatus();  // 副作用！
    List<BorrowDTO> list = reminderService.getReminderList();
    ...
}
```

每次刷新仪表盘都触发逾期状态更新，本应仅由凌晨定时任务执行的批量UPDATE在一天内可能运行成百上千次。

---

### 7.14 🟡 中危 — 前后端时区不一致风险

- MySQL连接的时区为`Asia/Shanghai`
- Java `LocalDateTime.now()`使用应用服务器时区
- 前端的`formatDate`不做时区转换

若应用服务器与数据库时区不同，`DATEDIFF`计算结果与实际借阅天数可能偏差一天。

---

### 7.15 🟡 中危 — 删除操作成功/失败均返回成功消息

**文件**：`controller/BookController.java:delete()` / `UserController.java:delete()`

```java
bookService.deleteBook(id);  // 未检查返回值
result.put("code", 200);
result.put("message", "删除成功");
```

即使`removeById`因记录不存在返回false，前端仍显示"删除成功"。

---

### 7.16 🟡 中危 — `phone`字段无唯一约束导致登录歧义

**文件**：`resources/db/schema.sql:tb_user`

`phone`无UNIQUE约束。若两个用户有相同手机号，`findByPhone`只返回第一条（MyBatis-Plus `getOne`默认行为），后一个用户永远无法登录。

---

### 7.17 🟡 中危 — 无全局异常处理器

项目中无`@ControllerAdvice`或`@RestControllerAdvice`，异常处理分散在各个Controller中，不一致且易遗漏。

---

### 7.18 🟡 中危 — `BorrowerController`绕过Service层直接调用Mapper

**文件**：`controller/BorrowerController.java`

```java
@Autowired
private BorrowRecordMapper borrowRecordMapper;  // 直接注入Mapper
```

破坏分层架构，无事务管理、无业务逻辑校验。

---

### 7.19 🟡 中危 — 仪表盘「借阅中数量」统计错误

**文件**：`views/Dashboard.vue`

```javascript
this.stats.borrowingCount = this.reminders.length
```

将逾期提醒列表长度当作「借阅中」数量，实际含义是「即将到期/已逾期记录数」，远小于真实借阅中数量。

---

### 7.20 🟢 低危 — `formatDate`函数在4个组件中重复定义

**文件**：`Dashboard.vue`, `BorrowList.vue`, `BorrowForm.vue`, `MyBorrowRecords.vue`

应提取为公共工具函数`@/utils/date`。

---

### 7.21 🟢 低危 — 全量加载无分页

**问题描述**：所有列表接口返回全量数据，前端不设分页。数据量大时前端卡顿、后端内存暴涨。

涉及接口：`GET /api/book/list`, `GET /api/user/list`, `GET /api/borrow/list`

---

### 7.22 🟢 低危 — MyBatis SQL日志开启在生产环境

```yaml
log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

所有SQL及参数打印到stdout，存在敏感信息泄露和安全风险。

---

### 7.23 🟢 低危 — 数据库密码明文存储在配置文件

```yaml
spring:
  datasource:
    password: 123456789
```

应使用环境变量或配置中心注入。

---

### 7.24 🟢 低危 — `remainingDays`为空时的前端空指针风险

前端`MyBorrowRecords.vue`中：
```javascript
Math.abs(scope.row.remainingDays)
```

若数据库`due_date`为NULL，`DATEDIFF`返回NULL，`Math.abs(null)`→`0`（不会崩溃但结果显示错误）。但若使用了`v-if`/`v-else`中引用属性，可能产生不正确的渲染。

---

### 7.25 🟢 低危 — `DataInitializer`默认管理员密码硬编码

```java
admin.setPassword(passwordEncoder.encode("admin123"));
```

部署后如果未修改，攻击者可尝试默认密码登录。

---

## 八、修复优先级建议

| 优先级 | 序号 | 易错点 | 影响范围 |
|--------|------|--------|---------|
| P0 | 7.2 | 后端无角色授权 | 安全 — 借阅者可操作管理员接口 |
| P0 | 7.1 | 借阅/归还并发竞争 | 数据 — 超借/库存丢失 |
| P0 | 7.3 | 客户端可设availableCount | 数据 — 库存不一致 |
| P1 | 7.4 | JWT密钥硬编码 | 安全 — Token可被伪造 |
| P1 | 7.5 | CORS全放通 | 安全 — CSRF攻击 |
| P1 | 7.6 | 登出不失效Token | 安全 — Token无法回收 |
| P1 | 7.7 | 登录无限速 | 安全 — 暴力破解 |
| P1 | 7.8 | 无服务端校验 | 数据 + 安全 |
| P1 | 7.9 | 异常无日志 | 运维 — 故障排查困难 |
| P2 | 7.10 | 借阅不验用户 | 数据 — 孤记录 |
| P2 | 7.12 | 删除图书后归还丢失库存 | 数据 — 库存失真 |
| P2 | 7.13 | GET有副作用 | 性能 + 设计 |
| P2 | 7.16 | phone无唯一约束 | 数据 + 功能 |
| P3 | 7.19 | 仪表盘统计错误 | 功能 |
| P3 | 7.21 | 无分页 | 性能 |
