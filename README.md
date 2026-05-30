# 🎓 CampusMart 校园闲置交易平台

> 为毕业生和学生提供便捷的二手物品交易服务，让闲置物品找到新主人。

![Vue 3](https://img.shields.io/badge/Vue-3.5+-42b883?style=flat&logo=vuedotjs)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=flat&logo=springboot)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6?style=flat&logo=typescript)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql)
![License](https://img.shields.io/badge/License-MIT-blue)

## ✨ 功能特色

### 🛒 交易功能
- **商品发布与管理** - 图片上传、分类选择、价格设置、库存管理
- **商品搜索与浏览** - 关键词搜索、分类筛选、热门/最新推荐
- **购物车系统** - 添加商品、修改数量、一键结算
- **订单流程** - 下单 → 支付（支付宝沙箱）→ 发货 → 确认收货
- **地址管理** - 收货地址 CRUD、默认地址设置

### 🤖 AI 智能客服
- **MiMo 大模型** - 接入小米 MiMo-v2.5 大语言模型
- **联网搜索** - 自动识别需要联网的问题，通过 Bing/百度搜索补充信息
- **LaTeX 数学公式** - 支持 `$$...$$` 和 `$...$` 语法渲染
- **Markdown 渲染** - 支持标题、列表、加粗、代码块等格式
- **对话历史持久化** - 按用户保存聊天记录，刷新不丢失

### 🎨 交互动效
- **动画角色登录页** - 4个彩色几何角色，眼睛跟随鼠标移动
- **智能对话气泡** - 角色根据用户行为弹出不同内容的对话气泡
- **可拖拽客服窗口** - 支持拖拽移动和调整大小

### 👤 用户系统
- **JWT 认证** - Token 登录、自动过期、Tab 级别隔离
- **用户中心** - 个人信息编辑、头像上传
- **私信系统** - 用户间即时消息、未读提醒

### 🔧 管理后台
- **仪表盘** - ECharts 数据可视化（销售额、订单量、用户趋势、分类占比）
- **商品审核** - 上架/下架/驳回管理
- **用户管理** - 启用/禁用用户
- **订单管理** - 查看订单详情、发货状态
- **分类管理** - 商品分类 CRUD

## 📸 项目截图

> 请在此处添加项目截图

| 首页 | 商品详情 | 管理后台 |
|:---:|:---:|:---:|
| ![首页](screenshot-home.png) | ![详情](screenshot-detail.png) | ![后台](screenshot-admin.png) |

## 🛠️ 技术栈

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5+ | 响应式 UI 框架 |
| TypeScript | 5.0+ | 类型安全 |
| Element Plus | 2.5+ | UI 组件库 |
| Pinia | 3.0+ | 状态管理 |
| Vue Router | 4.3+ | 路由管理 |
| Axios | 1.6+ | HTTP 请求 |
| ECharts | 6.0+ | 数据可视化 |
| KaTeX | latest | LaTeX 公式渲染 |
| Marked | latest | Markdown 渲染 |
| Vite | 8.0+ | 构建工具 |

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.0 | Web 框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 数据库 |
| JWT (jjwt) | 0.12.3 | 身份认证 |
| Lombok | 1.18 | 代码简化 |
| Spring Security | 6.2 | 密码加密 |

## 🚀 快速开始

### 环境要求

- **JDK 17+**
- **Node.js 18+**
- **MySQL 8.0+**
- **Maven 3.8+**

### 1. 克隆项目

```bash
git clone https://github.com/jiuming888/CampusMart.git
cd CampusMart
```

### 2. 数据库配置

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE campus_mart DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据（如果有 SQL 文件）
# source database/init.sql
```

修改后端数据库连接信息：

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_mart?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`

### 4. 启动前端

```bash
# 在项目根目录
npm install
npm run dev
```

前端启动在 `http://localhost:5175`

### 5. 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | 注册即可 | - |

## ⚙️ 配置说明

### AI 智能客服配置

在 `backend/src/main/resources/application.yml` 中配置：

```yaml
chatbot:
  # MiMo API 密钥（从 https://platform.xiaomimimo.com 获取）
  api-key: ${CHATBOT_API_KEY:你的密钥}
  # API 地址
  base-url: ${CHATBOT_BASE_URL:https://api.xiaomimimo.com/anthropic}
  # 模型名称
  model: ${CHATBOT_MODEL:mimo-v2.5}
```

也可以通过环境变量配置（推荐生产环境使用）：

```bash
export CHATBOT_API_KEY=你的密钥
export CHATBOT_BASE_URL=https://api.xiaomimimo.com/anthropic
export CHATBOT_MODEL=mimo-v2.5
```

### 支付宝沙箱配置（可选）

```yaml
alipay:
  app-id: 你的沙箱appId
  private-key: 你的应用私钥
  alipay-public-key: 支付宝公钥
  gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do
  return-url: http://localhost:8080/api/alipay/return
  notify-url: http://localhost:8080/api/alipay/notify
```

### 文件上传配置

```yaml
app:
  upload:
    path: ../public/images/    # 上传路径
    base-url: /images/         # 访问路径
```

## 📁 项目结构

```
CampusMart/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/campusmart/
│   │   ├── controller/         # REST 控制器
│   │   ├── service/            # 业务逻辑
│   │   ├── entity/             # 数据库实体
│   │   ├── mapper/             # MyBatis Mapper
│   │   ├── dto/                # 数据传输对象
│   │   ├── config/             # 配置类
│   │   ├── common/             # 通用工具
│   │   ├── interceptor/        # JWT 拦截器
│   │   └── util/               # 工具类
│   └── src/main/resources/
│       └── application.yml     # 配置文件
├── src/                        # Vue 3 前端
│   ├── api/                    # API 接口封装
│   ├── assets/                 # 样式、图片
│   ├── components/             # 公共组件
│   │   ├── AnimatedCharacters.vue  # 登录页动画角色
│   │   └── ChatWidget.vue     # AI 客服组件
│   ├── router/                 # 路由配置
│   ├── stores/                 # Pinia 状态
│   ├── utils/                  # 工具函数
│   └── views/                  # 页面组件
│       ├── admin/              # 管理后台页面
│       ├── Home.vue            # 首页
│       ├── Login.vue           # 登录页
│       ├── Register.vue        # 注册页
│       ├── ProductDetail.vue   # 商品详情
│       ├── Cart.vue            # 购物车
│       ├── Order.vue           # 订单
│       └── ...
├── public/                     # 静态资源
│   └── images/avatars/         # 用户头像
└── database/                   # 数据库脚本
```

## 🔌 API 接口

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 用户 | `/api/user/login` | POST | 用户登录 |
| 用户 | `/api/user/register` | POST | 用户注册 |
| 用户 | `/api/user/info` | GET | 获取用户信息 |
| 商品 | `/api/product/list` | GET | 商品列表 |
| 商品 | `/api/product/detail/{id}` | GET | 商品详情 |
| 商品 | `/api/product/publish` | POST | 发布商品 |
| 购物车 | `/api/cart/list` | GET | 购物车列表 |
| 购物车 | `/api/cart/add` | POST | 添加到购物车 |
| 订单 | `/api/order/create` | POST | 创建订单 |
| 订单 | `/api/order/list` | GET | 订单列表 |
| 私信 | `/api/message/send` | POST | 发送私信 |
| AI客服 | `/api/chatbot/chat` | POST | 智能对话 |
| 管理 | `/api/admin/dashboard/stats` | GET | 仪表盘数据 |

## 🌐 部署说明

### 生产环境配置

1. **前端构建**
```bash
npm run build  # 输出到 dist/
```

2. **后端打包**
```bash
cd backend
mvn clean package -DskipTests  # 输出 backend/target/*.jar
```

3. **环境变量**
```bash
export CHATBOT_API_KEY=你的密钥
export CHATBOT_BASE_URL=https://api.xiaomimimo.com/anthropic
export CHATBOT_MODEL=mimo-v2.5
export FRONTEND_URL=https://你的域名
```

4. **启动**
```bash
java -jar backend/target/campus-mart-1.0.0.jar
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

MIT License

## 致谢

- [Vue 3](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [MiMo LLM](https://github.com/XiaomiMiMo/MiMo)
- [Animated Characters](https://github.com/arsh342/careercompass)
