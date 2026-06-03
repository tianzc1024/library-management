# library-management

图书管理系统，Spring Boot + Vue 2 前后端分离架构，包含管理员端（B端）和借阅者端（C端）。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7 |
| 安全框架 | Spring Security + JWT (jjwt 0.11) |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0（生产）/ H2（测试） |
| 前端框架 | Vue 2 + Element UI |
| 构建工具 | Maven（后端）/ Vue CLI（前端） |

## 项目结构

```
library-management/
├── library-server/                         # 后端模块
│   └── src/main/java/com/library/
│       ├── LibraryApplication.java         # 启动类
│       ├── common/
│       │   ├── Result.java                 # 统一响应包装
│       │   └── GlobalExceptionHandler.java  # 全局异常处理
│       ├── config/
│       │   ├── SecurityConfig.java         # Spring Security 配置
│       │   ├── DataInitializer.java        # 默认管理员初始化
│       │   └── MybatisPlusConfig.java      # MyBatis-Plus 分页插件
│       ├── controller/
│       │   ├── AuthController.java         # 管理员认证接口
│       │   ├── BorrowerAuthController.java  # C端借阅者认证接口
│       │   ├── BookController.java         # 图书管理接口
│       │   ├── UserController.java         # 人员管理接口
│       │   ├── BorrowController.java       # 借阅/归还/统计接口
│       │   └── BorrowerController.java      # C端借阅者接口
│       ├── dto/
│       │   ├── BorrowDTO.java              # 借阅数据传输对象
│       │   ├── LoginRequest.java           # 登录请求
│       │   └── LoginResponse.java          # 登录响应
│       ├── entity/
│       │   ├── Book.java                   # 图书实体 (tb_book)
│       │   ├── User.java                   # 借阅者实体 (tb_user)
│       │   ├── SystemUser.java             # 系统管理员实体 (tb_system_user)
│       │   └── BorrowRecord.java           # 借阅记录实体 (tb_borrow_record)
│       ├── mapper/
│       │   ├── BookMapper.java
│       │   ├── UserMapper.java
│       │   ├── SystemUserMapper.java
│       │   └── BorrowRecordMapper.java
│       ├── security/
│       │   ├── JwtUtils.java               # JWT 生成/校验工具
│       │   ├── JwtAuthenticationFilter.java # JWT 认证过滤器
│       │   ├── TokenBlacklist.java         # Token 黑名单（登出失效）
│       │   └── RateLimiter.java            # 登录频率限制
│       ├── service/                        # 服务接口
│       └── task/
│           └── ReminderTask.java           # 逾期定时任务（每日2:00）
├── library-web/                            # 前端模块
│   └── src/
│       ├── api/                            # API 请求模块
│       │   ├── auth.js                     # 管理员认证
│       │   ├── borrower.js                 # 借阅者认证&查询
│       │   ├── book.js                     # 图书 CRUD
│       │   ├── user.js                     # 人员 CRUD
│       │   └── borrow.js                   # 借阅/归还/统计
│       ├── router/index.js                 # 路由配置 + 权限守卫
│       ├── utils/request.js                # Axios 实例 + 拦截器
│       ├── views/
│       │   ├── Login.vue                   # 管理员登录
│       │   ├── Dashboard.vue               # 首页仪表盘
│       │   ├── BookList.vue                # 图书管理
│       │   ├── UserList.vue                # 人员管理
│       │   ├── BorrowList.vue              # 借阅记录
│       │   ├── BorrowForm.vue              # 借阅/归还操作
│       │   └── borrower/
│       │       ├── BorrowerLogin.vue        # C端登录
│       │       └── MyBorrowRecords.vue      # 我的借阅记录
│       └── App.vue                         # 根组件（双布局）
└── pom.xml                                 # 父 POM
```

## 数据库表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `tb_book` | 图书表 | id, name, author, isbn, publisher, category, total_count, available_count |
| `tb_user` | 借阅者表 | id, name, phone(UNIQUE), email, address, password(BCrypt) |
| `tb_system_user` | 管理员表 | id, username(UNIQUE), password(BCrypt), real_name, role, status |
| `tb_borrow_record` | 借阅记录表 | id, user_id, book_id, borrow_date, due_date, return_date, status(0/1/2) |

---

## API 接口列表

> 所有业务接口响应格式 `{"code": 200, "data": ..., "message": "..."}`
> 认证接口响应格式 `{"code": 200, "message": "success", "data": {...}}`

### 1. 管理员认证

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 管理员登录（用户名+密码） | 公开 |
| GET | `/api/auth/current-user` | 获取当前登录用户信息 | Admin |
| POST | `/api/auth/logout` | 退出登录（Token加入黑名单） | Admin |

**登录请求体：**
```json
{ "username": "admin", "password": "admin123" }
```

**登录响应：**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbG...",
    "username": "admin",
    "realName": "系统管理员",
    "role": "ADMIN"
  }
}
```

### 2. 借阅者认证（C端）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/borrower/login` | 借阅者登录（手机号+密码） | 公开 |

**登录请求体：**
```json
{ "phone": "13800138000", "password": "123456" }
```

**登录响应：**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbG...",
    "phone": "13800138000",
    "name": "张三",
    "userId": 1,
    "userType": "BORROWER"
  }
}
```

### 3. 图书管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/book/list` | 获取图书列表 | Admin |
| GET | `/api/book/{id}` | 根据ID获取图书 | Admin |
| POST | `/api/book/add` | 添加图书 | Admin |
| PUT | `/api/book/update` | 更新图书 | Admin |
| DELETE | `/api/book/delete/{id}` | 删除图书（返回404如不存在） | Admin |

**添加图书请求体：**
```json
{
  "name": "Spring实战",
  "author": "Craig Walls",
  "isbn": "978-7-115-52122-4",
  "publisher": "人民邮电出版社",
  "category": "编程",
  "totalCount": 5
}
```
> 注：`availableCount` 由服务端自动设为 `totalCount`，不接受客户端传入。

### 4. 人员管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/user/list` | 获取人员列表 | Admin |
| GET | `/api/user/{id}` | 根据ID获取人员 | Admin |
| POST | `/api/user/add` | 添加借阅者 | Admin |
| PUT | `/api/user/update` | 更新借阅者信息 | Admin |
| DELETE | `/api/user/delete/{id}` | 删除借阅者（返回404如不存在） | Admin |

**添加人员请求体：**
```json
{
  "name": "张三",
  "phone": "13800138000",
  "email": "zhangsan@test.com",
  "address": "北京市朝阳区",
  "password": "123456"
}
```
> 注：`password` 由服务端 BCrypt 加密存储。更新时留空则不修改密码。`phone` 唯一。

### 5. 借阅/归还

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/borrow/borrow` | 借书（原子扣减库存） | Admin |
| POST | `/api/borrow/return/{recordId}` | 还书（原子恢复库存） | Admin |
| GET | `/api/borrow/list` | 全部借阅记录（含用户+图书信息） | Admin |
| GET | `/api/borrow/reminders` | 即将到期(≤3天) + 已逾期记录 | Admin |
| GET | `/api/borrow/stats` | 借阅统计（当前借阅中数量） | Admin |

**借书请求体：**
```json
{ "userId": 1, "bookId": 1, "borrowDays": 30 }
```

**stats响应：**
```json
{ "code": 200, "data": { "borrowingCount": 12 } }
```

### 6. 借阅者 C端

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/borrower/my-records` | 我的借阅记录（JWT隔离） | Borrower |

**响应：**
```json
{
  "code": 200,
  "data": [
    {
      "recordId": 1,
      "bookName": "Spring实战",
      "bookAuthor": "Craig Walls",
      "bookIsbn": "978-7-115-52122-4",
      "borrowDate": "2024-06-01T10:00:00",
      "dueDate": "2024-07-01T10:00:00",
      "returnDate": null,
      "status": 0,
      "remainingDays": 28
    }
  ]
}
```

---

## 前端路由与权限

| 路径 | 页面 | 权限 |
|------|------|------|
| `/login` | 管理员登录 | 公开 |
| `/` | 仪表盘（统计+逾期提醒） | Admin |
| `/book` | 图书管理 | Admin |
| `/user` | 人员管理 | Admin |
| `/borrow-list` | 借阅记录 | Admin |
| `/borrow` | 借阅/归还操作 | Admin |
| `/borrower/login` | C端借阅者登录 | 公开 |
| `/borrower/my-records` | 我的借阅记录 | Borrower |

> 路由守卫同时校验 `token` 和 `userType`，Admin 与 Borrower 互相隔离。

---

## 环境配置

| Profile | 数据库 | SQL日志 | 敏感信息 |
|---------|--------|---------|---------|
| `dev`（默认） | MySQL localhost | stdout | 配置文件明文 |
| `test` | H2 内存库 | 关闭 | 配置文件固定值 |
| `prod` | MySQL | 关闭 | 环境变量注入 |

```bash
# 开发
java -jar library-server.jar

# 生产（必须设置环境变量）
JWT_SECRET=<密钥> DB_PASSWORD=<密码> CORS_ORIGINS=<前端域名> \
  java -jar library-server.jar --spring.profiles.active=prod

# 测试
mvn test
```

### 生产环境变量

| 变量 | 说明 | 必填 |
|------|------|------|
| `JWT_SECRET` | JWT 签名密钥 | 是 |
| `DB_URL` | 数据库连接地址 | 否（有默认） |
| `DB_USERNAME` | 数据库用户名 | 否（有默认） |
| `DB_PASSWORD` | 数据库密码 | 是 |
| `CORS_ORIGINS` | 允许的前端域名 | 是 |

---

## 启动步骤

### 1. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS library_db DEFAULT CHARACTER SET utf8mb4;
-- 执行 src/main/resources/db/schema.sql 建表
```

### 2. 后端

```bash
cd library-server
mvn spring-boot:run
# 默认管理员: admin / admin123
```

### 3. 前端

```bash
cd library-web
npm install
npm run serve       # 开发模式
npm run build       # 生产构建
```

---

## JWT Token 结构

| Claim | 说明 | 示例值（Admin） | 示例值（Borrower） |
|-------|------|-----------------|-------------------|
| `sub` | 主体标识 | `admin` | `13800138000` |
| `role` | 角色 | `ADMIN` | `BORROWER` |
| `userId` | 用户ID | `null` | `123` |
| `userType` | 用户类型 | `ADMIN` | `BORROWER` |
| `iat` | 签发时间戳 | `1717286400` | `1717286400` |
| `exp` | 过期时间戳 | `1717372800` | `1717372800` |

- 有效期: 24 小时（86400000ms）
- 登出后 Token 加入内存黑名单，即时失效
- 登录频率限制: 同 IP 60 秒内最多 5 次尝试

---

## 借阅状态枚举

| status | 含义 |
|--------|------|
| 0 | 借阅中 |
| 1 | 已归还 |
| 2 | 已逾期（定时任务每日 2:00 自动标记） |

---

## 安全防护

| 措施 | 说明 |
|------|------|
| 密码加密 | BCrypt 加密存储（管理员 + 借阅者） |
| JWT 认证 | 所有业务接口需携带 `Authorization: Bearer <token>` |
| 角色管控 | Admin 和 Borrower 接口完全隔离 |
| Token 黑名单 | 登出后 Token 即时失效 |
| 登录限流 | 同一 IP 60 秒内最多 5 次登录尝试 |
| 并发安全 | 借阅/归还使用原子 SQL（`UPDATE ... SET count = count ± 1`） |
| 全局异常处理 | 统一 `@RestControllerAdvice`，异常自动日志记录 |
| 生产环境 | 敏感信息全部通过环境变量注入 |
