一个基于 Spring Boot构建的现代化任务管理平台，集成了 AI 智能评分、抢单机制、实时通知等高级功能。

## ✨ 核心特性

- **🔐 安全认证**：JWT + Spring Security 实现的双令牌认证机制（Access Token + Refresh Token）
- **🤖 AI 智能评分**：基于阿里云百炼大模型的自动作业评分与查重系统
- **⚡ 抢单系统**：基于 Redis + Lua 脚本的高并发抢单机制，支持自动释放未完成任务
- **📊 排行榜系统**：实时积分排行榜，支持多级缓存（Redis + Caffeine）
- **🔔 实时通知**：WebSocket 实现的成绩实时推送
- **📁 文件存储**：支持阿里云 OSS 和腾讯云 COS 双云存储
- **📤 Excel 导出**：基于 EasyExcel 的用户数据导出功能
- **⏰ 定时任务**：基于 Spring Scheduler 的任务提醒与超时处理
- **🐇 消息队列**：RabbitMQ 实现异步任务处理（AI 分析、抢单消息等）
- **📝 签到系统**：用户每日签到积分管理

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.4, Spring Security, Spring Data JPA |
| 缓存 | Redis, Redisson, Caffeine |
| 消息队列 | RabbitMQ |
| AI 集成 | LangChain4j, 阿里云百炼 SDK |
| 存储 | 阿里云 OSS, 腾讯云 COS |
| 文档处理 | Apache POI, PDFBox, EasyExcel |
| 其他 | JWT, WebSocket, Lombok |
