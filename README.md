# 轻幕 · Lightframe TV

[English](README.en.md)

**无首页，开机直达你想看的 App。**

[下载最新 Release](https://github.com/techfanseric/lightframe-tv/releases/latest)

轻幕不提供单独的桌面首页。开机后直接进入视频 App；观看时按首页键，只唤出覆盖在当前画面上的轻量切换菜单，选好应用即可直接切换。

![轻幕：视频播放画面上打开应用切换菜单](docs/images/launcher-menu.png)

电视实机旧版截图：展示首页键菜单覆盖在视频画面上的效果。图中的广告声音开关已移除；v0.1.5 新增的智能家居入口和可选快捷行未出现在此图中。

当前版本：[v0.1.5](https://github.com/techfanseric/lightframe-tv/releases/tag/v0.1.5) · Home 8.4（12）／Overlay 8.22（31）。完整变更见 [更新记录](CHANGELOG.md)。

## YouTube 客户端

从 **v0.1.1** 起，轻幕中的 YouTube 统一使用 [TizenTube Cobalt](https://github.com/reisxd/TizenTubeCobalt)：应用切换、开机指定应用、跟随上次启动和当前应用识别都指向同一个客户端。菜单与开机设置显示“TizenTube”，应用入口使用独立的青绿色 T 标识，避免与官方 YouTube 混淆。

请另行安装 TizenTube Cobalt（当前验证版本为 2.0.3）。原官方 YouTube 可保留作手动回退，轻幕不会自动切回官方客户端。用户本次试用确认播放体验正常、未见广告；这不代表对所有视频都保证无广告。

菜单保留**应用＋设置两行**，并可在应用行上方显示智能家居快捷行；已移除“关闭广告声音”及其菜单网络调用。安装更新时，贴片会在后台恢复服务，不主动唤出菜单或切换视频；HOME 交互与开机恢复仍由用户在方便时验收。

## IPTV

从 **v0.1.4** 起加入 IPTV 应用入口，使用“我的电视”播放器，支持开机选择、最近使用排序和当前应用排除。安装包下载、公开节目源准备和导入步骤见 [IPTV 配置](docs/iptv.md)。支持电视未开机时先在电脑准备，开机后安装；播放器和节目源不打包进轻幕 Release。

**v0.1.5** 补充中文主分类、经核对的频道中文名和独立的按地区节目源；每组采用中文常看台优先的编辑排序，同频道的不同线路相邻。导入时保留 EPG 查询名并迁移收藏名称；`--keep-current` 保持当前源选择，`prepare-iptv.py --offline` 使用本地快照重新生成节目单。源条目数量不代表已验证可播放，排序不代表实时收视率。

## 智能家居

从 **v0.1.5** 起，设置行的“智能家居”可连接 Home Assistant，按房间浏览设备、控制和状态；未分配房间、离线、禁用及暂无实体的设备也保留在目录中。支持空调开关和摄氏温度调节、窗帘开关与停止、灯／风扇／开关，以及扫地机器人开始、暂停和回充；其他实体按只读状态显示。

在面板中选择并保存常用控制或状态，才会出现在应用行上方。每行最多四项、最多三行，实际数量按屏幕高度限制；打开菜单仍默认聚焦应用行。连接凭据通过本地配置脚本导入，token 加密保存在应用私有目录，配置文件不进入仓库。配置与验证方法见 [Home Assistant 接入](docs/home-assistant.md)。

遥控器采音仅提供手动诊断；中文语音识别和 Assist 语音控制尚未完成验证。设备状态读取和指令发送反馈也不替代实际设备动作验收。

## 核心特点

- **无独立首页**：无需先回桌面再找应用，电视始终围绕你正在使用的 App。
- **开机直达指定 App**：可选择 TizenTube、哔哩哔哩、Netflix 或 IPTV，也可跟随上次使用的应用；默认跟随上次，自动打开可单独关闭。
- **视频中直接切换**：首页键唤出应用切换菜单，隐藏当前 App、按最近使用排序，默认选中上一个 App；左右键循环，上下切行。
- **自动检测和启动代理**：检测已配置的 FlClash VPN 状态，未连接时尝试启动代理；打开 YouTube / Netflix 前检查控制器与目标网络是否就绪，避免开机网络尚未连通时直接卡在应用中。无需代理的 B 站直接打开。
- **网络状态一眼可见**：顶部小状态栏动态显示代理节点、延迟、上传/下载速率及流量曲线，不占用一整块画面。
- **状态栏智能显示**：默认在 B 站前台自动隐藏，离开后恢复；也可选择常开或常关，并记住选择。
- **常用设置直接可达**：信号源、开机应用、开机启动、状态栏、FlClash 和系统设置横向列出，图标在左、文字在右。
- **智能家居快捷控制**：设置行进入原生 Home Assistant 面板，按房间浏览完整设备目录，选择的常用控制或状态显示在应用行上方，每次打开仍默认聚焦应用行。使用 Lucide 图标，支持设备状态、空调调温、窗帘和扫地机器人等控制；配置方法及语音验证边界见 [智能家居](docs/home-assistant.md)。
- **克制的视觉层次**：左侧居中的连续半透明衬底，仅焦点项有底色，减少贴片和边框干扰。
- **原生信号源集成**：针对已验证的小米固件，支持信号源选择和 HDMI 接入自动切换。

代理自动启动依赖事先配置好的 FlClash、订阅和系统 VPN 授权；轻幕不会代为提供代理线路。视频应用和会员账号也需另行安装、登录。

## 下载与安装

从 [Release](https://github.com/techfanseric/lightframe-tv/releases/latest) 下载 **LightframeHome.apk** 和 **LightframeOverlay.apk**，两者需配套安装：前者接管首页键，后者提供切换菜单和状态栏。安装命令见下方；直接使用 Release APK 不需要 Android SDK 构建环境。

## 遥控器

| 按键 | 行为 |
| --- | --- |
| 首页 | 唤出 / 收起菜单 |
| 左右 | 当前行循环切换 |
| 上下 | 在家居快捷行、应用行与设置行之间移动 |
| 确认 | 打开应用或执行当前选项 |
| 返回 | 收起切换菜单；家居设备与控制页逐级返回 |
| 菜单 | 聚焦设置行 |

## 构建

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0、Python 3 和 `zip`。

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
./scripts/build.sh
```

输出为 `dist/LightframeHome.apk` 和 `dist/LightframeOverlay.apk`。也可通过 `LIGHTFRAME_BUILD_TOOLS`、`LIGHTFRAME_ANDROID_JAR` 指定现有工具路径。

两个 APK 必须使用同一签名。构建脚本会为新 checkout 在 `.local/signing.jks` 创建开发密钥；更新已安装版本须沿用原密钥。可用 `LIGHTFRAME_KEYSTORE`、`LIGHTFRAME_KEY_ALIAS`、`LIGHTFRAME_STORE_PASSWORD`、`LIGHTFRAME_KEY_PASSWORD` 指定签名。私钥和构建产物不进入 Git。

## 安装

目标电视须开启 ADB，且支持允许悬浮窗和使用情况访问。请先构建，再设置设备序列号：

```sh
export TV_ADB_SERIAL=your-tv-ip:5555
./scripts/install.sh "$TV_ADB_SERIAL" --set-home
```

`--set-home` 将此应用设为默认桌面；不传则只安装和授权。脚本不重启、不清数据，也不禁用原桌面。安装完成后，准备好时用遥控器按首页键。

TizenTube Cobalt、哔哩哔哩、Netflix、FlClash VPN、订阅和会员登录需另行准备，本仓库不分发这些内容。Clash 控制器接入见 [集成配置](docs/integrations.md)。

## 结构与兼容性

- `apps/home`：透明 HOME 转接入口、启动与原生系统功能桥接。
- `apps/overlay`：独立悬浮服务、菜单、状态栏、使用记录及网络门控。
- `scripts`：共用构建、安装和本地凭据配置。

两包用签名权限通信，保留 `local.simple.tvhome` / `local.simple.tvoverlay` 包名以兼容已有安装与偏好。已验证设备为 Android 9 / API 28、ARM32 小米电视（alita / MiTV-MFTP0）；其他固件的输入源、原生设置和按键行为需实机验证。

当前背景是局部渐隐衬底，不是跨应用实时毛玻璃。它不会采集屏幕或逐帧做图像滤镜。原生语音入口可唤起，但遥控器语音识别仍依赖设备原有服务，尚未解决其识别故障。第三方视频 App 的认证、DRM 与实际播放兼容性不由启动器保证。

## 第三方资源

图标使用 [Lucide](https://lucide.dev/)，ISC 许可随 SVG、来源清单和 APK 资源保留在 `apps/overlay/third-party/lucide/`。应用图标在运行时读取已安装应用。
