# Fork Notice / Fork 声明

[English](#english) | [中文](#中文)

---

## English

### Original Project

This repository is a fork of Datadog's **dd-sdk-android-gradle-plugin**.

- **Original Repository**: https://github.com/DataDog/dd-sdk-android-gradle-plugin
- **Original Copyright**: © 2019-2024 Datadog, Inc.
- **Original License**: Apache License 2.0

### Modifications

This fork has been modified by **Flashcat, Inc.** to integrate with the Flashcat platform. Key modifications include:

1. **Rebranding**: Changed from Datadog to Flashcat branding
2. **Site Endpoints**: Updated to use Flashcat service endpoints
   - `ci.flashcat.cloud` (CN site)
   - `ci-dev.flashcat.cloud` (STAGING site)
3. **Configuration**: Modified extension name from `datadog {}` to `flashcat {}`
4. **API Keys**: Updated environment variables to `FC_API_KEY` / `FLASHCAT_API_KEY`
5. **CI Configuration**: Changed JSON field from `datadogSite` to `flashcatSite`

### License Compliance

All modifications are made in compliance with the Apache License 2.0:

- ✅ Original LICENSE file is retained
- ✅ Original copyright notices are preserved in source files
- ✅ NOTICE file updated to acknowledge the original work
- ✅ Modified files carry the original Apache License header
- ✅ This derivative work is distributed under the same Apache License 2.0

### Attribution

We acknowledge and thank **Datadog, Inc.** for their excellent work on the original dd-sdk-android-gradle-plugin, which serves as the foundation for this project.

---

## 中文

### 原始项目

本仓库是 Datadog 的 **dd-sdk-android-gradle-plugin** 的 fork。

- **原始仓库**: https://github.com/DataDog/dd-sdk-android-gradle-plugin
- **原始版权**: © 2019-2024 Datadog, Inc.
- **原始许可证**: Apache License 2.0

### 修改内容

本 fork 由 **Flashcat, Inc.** 修改，以集成 Flashcat 平台。主要修改包括：

1. **品牌重命名**: 从 Datadog 改为 Flashcat 品牌
2. **服务端点**: 更新为使用 Flashcat 服务端点
   - `ci.flashcat.cloud` (CN 站点)
   - `ci-dev.flashcat.cloud` (STAGING 站点)
3. **配置方式**: Extension 名称从 `datadog {}` 改为 `flashcat {}`
4. **API 密钥**: 环境变量更新为 `FC_API_KEY` / `FLASHCAT_API_KEY`
5. **CI 配置**: JSON 字段从 `datadogSite` 改为 `flashcatSite`

### 协议合规性

所有修改均符合 Apache License 2.0 要求：

- ✅ 保留了原始的 LICENSE 文件
- ✅ 源代码文件中保留了原始版权声明
- ✅ 更新了 NOTICE 文件以致谢原始作品
- ✅ 修改的文件保留了原始 Apache License 头部注释
- ✅ 本衍生作品使用相同的 Apache License 2.0 发布

### 致谢

我们感谢并致敬 **Datadog, Inc.** 在原始 dd-sdk-android-gradle-plugin 上的出色工作，该项目是本项目的基础。

---

## License / 许可证

This project is licensed under the Apache License, Version 2.0.  
本项目使用 Apache License 2.0 许可证。

See [LICENSE](LICENSE) file for the full license text.  
完整许可证文本请查看 [LICENSE](LICENSE) 文件。

See [NOTICE](NOTICE) file for attribution details.  
归属详情请查看 [NOTICE](NOTICE) 文件。
