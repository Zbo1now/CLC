# 创联空间管理系统（Admin Web）

这是创联空间管理系统的前端项目，基于 Vue 3 + Vite + Element Plus 开发。

## 目录结构

- `src/layout`: 布局组件 (Sidebar, Header)
- `src/views`: 页面视图
- `src/router`: 路由配置
- `src/styles`: 全局样式

## 快速开始

### 1. 安装依赖

请确保你的 Node.js 版本 >= 16.0.0 (推荐使用 LTS 版本)。

```bash
cd admin-web
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

### 3. 构建生产版本

```bash
npm run build
```

## 后端接口

开发环境配置了代理，将 `/api` 开头的请求转发到 `http://localhost:8080`。
请确保后端服务已启动。
