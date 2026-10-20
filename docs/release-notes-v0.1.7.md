智能家居按空间和设备组组织，适合连续操作；Clash 智能显示支持信号源模式。

- 每空间一行、横向浏览组、纵向浏览更多空间。空间名固定在左侧，加粗并用细竖线区分层级；44dp 紧凑行配合家居区与应用区之间的分割线。
- 操作家居后保留主菜单、焦点和滚动位置，取消本次家居操作会话的自动收起。切换应用仍关闭菜单。
- 空调、窗帘、多路面板等使用与主菜单一致的定制控制子菜单；选定动作后关闭子菜单，不重新弹回组内列表。
- 支持在电视上调整空间与组的名称、顺序和显隐，通过本地 JSON 配置控制目标、明细和“先打开功能列表”。旧收藏布局仍兼容。
- 智能显示模式在 B 站和电视信号源模式下隐藏 Clash 状态栏，其他应用恢复；常开、常关设置保留。

Home **8.4 / code 12**；Overlay **8.24 / code 33**。两包沿用原有共享签名，包名和签名权限不变。配套安装 APK，使用 `SHA256SUMS.txt` 核对文件。

JDK 17 / SDK 35 构建、逻辑检查和 APK 签名检查通过。此前迭代已在电视后台更新，用户试用确认空间分组与连续操作体验；本版不以此声称所有设备动作或每种信源切换均已验收。发布过程未执行遥控器按键、开关机或设备控制。

个人 HA 预设、家庭实体清单、凭据和设备偏好均留在本地，不随源码或 Release 分发。首次使用请按 [Home Assistant 接入文档](https://github.com/techfanseric/lightframe-tv/blob/main/docs/home-assistant.md) 配置。

Adds configurable compact smart-home space/group rows with fixed bold headings, persistent main-menu controls and custom action submenus that close after selection. Smart Clash visibility now hides the status strip in TV input mode. Includes both signed APKs and SHA-256 checksums; private household presets and credentials are excluded.
