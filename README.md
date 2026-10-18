# 轻幕 · Lightframe TV

[English](README.en.md)

一个为遥控器和日常观看设计的轻量 Android TV 启动器：按首页键唤出菜单，切换应用、调整常用设置，然后回到视频。

<!-- SCREENSHOT: current video with the HOME menu open; awaiting a valid device capture. -->

## 使用体验

- 图标在左、文字在右，左侧居中的连续半透明菜单，仅焦点项有底色。
- 应用行展示 YouTube、哔哩哔哩、Netflix 中的其他应用，隐藏当前应用；按最近使用排序，左右循环。
- 设置行直接展示信号源、开机应用、开机启动、状态栏、FlClash、系统设置。
- 开机应用默认跟随上次；YouTube / Netflix 的自动启动等待网络就绪。
- Clash 顶部状态栏显示节点、延迟、上下行速率与曲线。默认**智能**：B 站前台隐藏，离开恢复；也可设为常开或常关。
- 仅在 YouTube 中，菜单上方显示“关闭广告声音”。开关默认关闭，焦点仍从应用行开始，按上进入。该开关控制已配置的外部广告助手；自动跳过和静音相互独立。
- HDMI 热插拔与信号源切换针对已验证的小米电视固件实现。

## 遥控器

| 按键 | 行为 |
| --- | --- |
| 首页 | 唤出 / 收起菜单 |
| 左右 | 当前行循环切换 |
| 上下 | 在应用、设置与 YouTube 专属行之间移动 |
| 确认 | 打开应用或执行当前选项 |
| 返回 | 收起菜单 |
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

已有 App、FlClash VPN、订阅和会员登录需另行配置，本仓库不分发这些内容。Clash 控制器接入及广告声音开关见 [集成配置](docs/integrations.md)。

## 结构与兼容性

- `apps/home`：透明 HOME 转接入口、启动与原生系统功能桥接。
- `apps/overlay`：独立悬浮服务、菜单、状态栏、使用记录及网络门控。
- `scripts`：共用构建、安装和本地凭据配置。

两包用签名权限通信，保留 `local.simple.tvhome` / `local.simple.tvoverlay` 包名以兼容已有安装与偏好。已验证设备为 Android 9 / API 28、ARM32 小米电视（alita / MiTV-MFTP0）；其他固件的输入源、原生设置和按键行为需实机验证。

当前背景是局部渐隐衬底，不是跨应用实时毛玻璃。它不会采集屏幕或逐帧做图像滤镜。原生语音入口可唤起，但遥控器语音识别仍依赖设备原有服务，尚未解决其识别故障。第三方视频 App 的认证、DRM 与实际播放兼容性不由启动器保证。

## 第三方资源

图标使用 [Lucide](https://lucide.dev/)，ISC 许可随 SVG、来源清单和 APK 资源保留在 `apps/overlay/third-party/lucide/`。应用图标在运行时读取已安装应用。
