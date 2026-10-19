新增 Home Assistant 智能家居控制，并完善 IPTV 节目单的中文分类、名称和排序。

- 设置行新增原生智能家居面板，按房间、设备、实体浏览目录；保留离线、禁用、未分配房间和暂无实体的设备。
- 可选常用控制或状态显示在应用行上方，每行最多四项、最多三行，按屏幕高度限制。保留应用行默认焦点、最近使用排序、当前应用排除、左右循环及状态栏智能显示。
- 支持空调开关与摄氏温度调节、窗帘开关与停止、灯／风扇／开关和扫地机器人控制；不支持操作的实体只显示状态。连接 token 通过本地导入并加密保存在应用私有目录。
- IPTV 增加中文主分类、经核对的中文台名、独立地区源及中文常看台优先的编辑排序；同频道的不同线路相邻。保留 EPG 查询名，导入时迁移收藏名称，支持保持当前源和本地快照离线重建。
- 同步更新中英文 README、配置文档与离线回归检查。

Home **8.4 / code 12**；Overlay **8.22 / code 31**。两个 APK 沿用 v0.1.4 的共享签名，包名和签名权限保持不变，可覆盖更新并保留设置。请配套安装，使用 `SHA256SUMS.txt` 核对文件。

验证包括 JDK 17 / SDK 35 构建、APK 签名及上一版证书比对、IPTV 离线回归、家居命令与导航逻辑、WebSocket 本地集成检查。本次发布未操作或安装到电视；实际设备动作、HOME 交互和开机恢复仍需在方便时验收。遥控器采音仅提供手动统计诊断，中文识别和 Assist 语音控制尚未验证完成；节目源条目和排序不代表播放保证或实时收视率。

配置说明：[智能家居](https://github.com/techfanseric/lightframe-tv/blob/v0.1.5/docs/home-assistant.md) · [IPTV](https://github.com/techfanseric/lightframe-tv/blob/v0.1.5/docs/iptv.md)。私密配置、设备偏好、第三方播放器和节目源快照不随 Release 分发。

Adds a native Home Assistant catalog and selected quick controls, plus reviewed Chinese IPTV categories/names, regional sources, editorial ordering and favorite-name migration. Includes both signed Lightframe APKs and SHA-256 checksums. Speech recognition, Assist voice control and individual stream playback remain unverified.
