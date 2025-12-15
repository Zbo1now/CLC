# CampusCoin H5 接口文档

## 运行环境
- Servlet 容器：Tomcat 9/10（或兼容容器）
- JDK 8+
- MySQL 8+

## 部署与端口
- 配置文件：`src/main/resources/application.yml`
  ```yaml
  server:
    port: 8081
  ```
- 注意：本项目使用外部 Servlet 容器（Tomcat 等），实际端口仍由容器 `conf/server.xml` 控制。请将 `application.yml` 的 `server.port` 与 Tomcat Connector 端口保持一致，例如：
  ```xml
  <Connector port="8081" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
  ```
- 部署方式：`mvn clean package` 后，将生成的 `campuscoin-h5.war` 放入 Tomcat `webapps/`，或使用 IDE 配置部署。
- 前端（JSP H5）与后端 Servlet 同容器同端口，入口 `http://<host>:<port>/campuscoin-h5/index.jsp`（若为 ROOT 部署，则 `http://<host>:<port>/index.jsp`）。

## 数据库
- 初始化脚本：`db/schema.sql`
- 连接配置：`src/main/resources/application.yml` 中 `datasource` 段落：
  ```yaml
  datasource:
    url: jdbc:mysql://localhost:3306/campuscoin?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: 123456
    driver: com.mysql.cj.jdbc.Driver
  ```

## 认证接口
### 注册
- `POST /api/auth/register`
- `Content-Type: application/json`
- Body
  ```json
  {
    "teamName": "string",      // 唯一团队名
    "password": "string",      // >=6 位，BCrypt 存储
    "contactName": "string",   // 联系人
    "contactPhone": "string"   // 联系方式
  }
  ```
- 响应（201）
  ```json
  {
    "success": true,
    "message": "Team registered successfully",
    "data": {
      "teamName": "xxx",
      "balance": 500
    }
  }
  ```
- 可能错误：400 参数不合法；409 团队名已存在；500 注册失败。

### 登录
- `POST /api/auth/login`
- `Content-Type: application/json`
- Body
  ```json
  {
    "teamName": "string",
    "password": "string"
  }
  ```
- 响应（200）
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "teamName": "xxx",
      "balance": 500
    }
  }
  ```
- 可能错误：400 参数缺失；401 认证失败。

## 会话
- 登录成功后创建 HttpSession，键：`teamId`、`teamName`。
- 如需鉴权接口，可检查 session 是否存在。

## 日志
- 使用 `java.util.logging`，配置文件 `logging.properties`。

## 前端入口与主题
- 页面：`index.jsp`（内含登录/注册表单、主题切换、响应式布局）。
- 前端调用：`fetch('/api/auth/...')` 使用 JSON 交互。
