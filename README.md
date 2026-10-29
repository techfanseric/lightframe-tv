# 轻幕 · Lightframe TV

[English](README.en.md) · [下载 v0.2.0](https://github.com/techfanseric/lightframe-tv/releases/tag/v0.2.0) · [安装指南](docs/install.md) · [回退与排错](docs/troubleshooting.md)

**电视围绕正在播放的应用，首页键只唤出轻量覆盖菜单。**

轻幕由透明 HOME 转接器和原生悬浮服务组成。开机可进入上次使用的视频应用；观看时按首页键，在小尺寸彩色磁片间切换。菜单关闭后回到原应用；是否继续或自动播放具体内容由视频客户端决定。

![轻幕紧凑应用栏](docs/images/dock-apps.png)

> 图片由真实 Android 原生界面在模拟器渲染，背景与家居名称是演示数据，不是电视播放截图。

| 设置：紧凑四列 | 智能家居：聚焦时展开 |
| --- | --- |
| ![设置面板](docs/images/dock-settings.png) | ![智能家居面板](docs/images/dock-home.png) |

## v0.2.0 的变化

- 居中小磁片、各自主题色、无可见入口名称；选中放大 12%，无描边。
- 底部深色渐隐遮罩，设置／家居面板按内容高度展开，底栏不随展开移动。
- 面板每行四项，使用 Phosphor Fill 实心图标；家居快捷区最多 15 个实体加全部设备入口。
- 左侧 24 小时日期时间与右侧 Clash 状态条统一透明样式，共用常开／常关／智能开关。
- 设置新增 ADB 页面快捷入口；小米走原生安全页，其他设备尝试 Android 开发者设置。入口不会自动开启 ADB。
- 新增跨平台安装向导、配套开源应用包、校验文件、可回退的首页激活流程。

## 先确认适用条件

需要 **Android 9 / API 28 及以上**、可用 ADB、悬浮窗及使用情况访问。当前实机验证为一台 Android 9 ARM32 小米电视；其他 Android TV／Google TV 是待验证设备，不保证所有厂商均允许替换 HOME 或后台悬浮。三星 Tizen、LG webOS 不能安装 Android APK。

**YouTube / Netflix 启动前的网络检查依赖已配置的 FlClash VPN、本机 9090 控制器与凭据。** 当前版本没有直接联网模式；仅装轻幕或 APK 合集还不能完成联网配置。订阅、VPN 系统授权、会员登录、Home Assistant 授权均由使用者自行完成。无需代理的 B 站可直接打开。

Netflix 的 DRM、设备认证和 Google 服务兼容性需要单独验证。附带的播放守护是针对已确认问题的可选组件，安装向导不会停用或安装 Google 服务。

## 最短安装路径

1. 从 Release 下载 `Lightframe-v0.2.0-setup.zip` 并解压。需要电脑上的 Python 3.9+ 与 Android Platform Tools（`adb`）。
2. 电视打开 ADB，与电脑同一可信局域网；较新系统先用 `adb pair IP:配对端口`，再连接实际调试端口。
3. 在解压目录执行检测和安装：

```sh
adb connect TV_IP:5555
python3 scripts/setup.py doctor --serial TV_IP:5555 --bundle .
python3 scripts/setup.py install --serial TV_IP:5555 --bundle .
```

4. 需要配套应用时，下载 Release 的 companions 包，解压至同一目录，再按[完整指南](docs/install.md)安装与配置。已有第三方客户端会跳过，不覆盖账号数据。
5. 配好网络，完成预检后，单独激活首页：

```sh
python3 scripts/setup.py activate --serial TV_IP:5555 --bundle .
```

Windows 可将 `python3` 换为 `py -3`。安装不会自动清数据、卸载应用或重启电视。完整的 ABI 选择、文件结构、首次授权、验收和回退见[安装指南](docs/install.md)。

## 应用与遥控器

| 入口 | 实际客户端 |
| --- | --- |
| YouTube | TizenTube Cobalt（界面无标签，辅助功能描述保留 TizenTube） |
| Netflix | Netflix TV `com.netflix.ninja`，或系统已有客户端 |
| 哔哩哔哩 | 云视听小电视 |
| IPTV | 我的电视 MyTV |

首页键唤出／收起；左右循环选项；设置或家居聚焦后，上键进入面板；确认打开应用／控制页；返回关闭；菜单键聚焦设置。当前视频应用会从入口行排除，其他应用按最近使用排序。

## 文档

- [安装、激活、升级与回退](docs/install.md)
- [Clash 与配套应用](docs/integrations.md)
- [Home Assistant 配置及操作边界](docs/home-assistant.md)
- [兼容性、Google 服务与播放守护](docs/compatibility.md)
- [排错与安全清理](docs/troubleshooting.md)
- [构建与发布维护](docs/build-release.md)
- [第三方来源与许可证](docs/third-party.md)
- [更新记录](CHANGELOG.md)

v0.2 使用 `local.simple.tvhome.dock` 与 `local.simple.tvoverlay.dock` 并行包名。旧版可保留数据用于回退；激活时只停用旧轻幕，避免两套服务同时开机。旧版加密凭据不能跨应用复制，家居与控制器需要重新配置。详情见安装指南。

## 隐私与授权

轻幕不采集视频画面，不实现跨应用实时模糊。HA token 加密保存在应用私有存储；控制器凭据留在应用私有配置。配置导入文件随后删除。Release 不含订阅、会员账号、家庭设备清单、私钥或已登录数据。

Phosphor Fill、部分 Lucide 与第三方品牌图形的来源和许可证随源码保留。YouTube／Netflix 等商标归各权利人，轻幕并非这些服务的官方产品。
