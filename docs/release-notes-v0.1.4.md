新增 IPTV 集成，方便在轻幕中直接进入电视直播播放器。

- 新增 **IPTV** 应用入口，使用单独安装的“我的电视”播放器；支持开机指定、跟随上次和当前应用识别。
- 应用排序、运行状态和当前应用排除扩展到全部已配置应用；应用行自适应分配宽度，保留默认焦点和左右循环导航。
- 提供播放器下载、公开节目源准备、安装和导入脚本；源列表去重并将中文频道放在前面，排除播放器无法处理的专用请求头条目。
- 修复播放器配置接口对空 GET 请求的兼容性；重复导入保留现有其他源、收藏及偏好。

Home **8.4 / code 12**；Overlay **8.17 / code 26**。两包与 v0.1.3 使用相同签名，可以覆盖安装并保留设置。请配套安装两个 APK；`SHA256SUMS.txt` 提供校验值。

构建、签名与上一版证书对比、脚本检查及导入回归检查通过。IPTV 已在目标 Android 9 小米电视完成安装和导入，合并节目单解析为 **178 个分组、10,630 条播放地址**，用户确认使用效果。此数量是准备时的源条目数，不代表所有频道均已验证可播放；地域、网络及源状态会影响可用性。

播放器 APK、节目源快照和电视配置不随 Release 分发。准备及导入方式见 [IPTV 配置说明](https://github.com/techfanseric/lightframe-tv/blob/v0.1.4/docs/iptv.md)。

Adds IPTV launcher integration, startup/MRU support and local preparation/import tools for public playlists. Includes both signed Lightframe APKs and SHA-256 checksums; third-party players and playlist snapshots remain separate.
