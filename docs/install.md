# 安装与回退（v0.2.0）

## 1. 下载与准备

Release 提供 `Lightframe-v0.2.0-setup.zip`（两只轻幕 APK、向导、指南、校验），以及 `Lightframe-v0.2.0-companions.zip`（TizenTube ARM32/64、FlClash ARM32/64、MyTV、可选播放守护）。配套包解压到 setup 同一目录，保留其 SHA256SUMS 文件。单独 APK 也同时提供。`third-party-sources.zip` 保存FlClash/MyTV对应源码快照、全部许可证及TizenTube源码下载地址。

电脑安装 Python 3.9+ 和 [官方 Android Platform Tools](https://developer.android.com/tools/releases/platform-tools)，让 `adb` 在 PATH 中，或设置 ADB 环境变量指向它。Windows PowerShell 使用 `py -3`，macOS/Linux 使用 `python3`。不要在已连接多台设备时省略目标序列号。

电视需 Android 9+，支持 ADB、悬浮窗及使用情况访问。经典网络 ADB 常用 `IP:5555`；Android 11+无线调试可能先要求 `adb pair IP:配对端口`，连接端口与配对端口不同。授权弹窗需在电视上确认。重启后是否保留网络 ADB 由电视固件决定。

## 2. 检测，再安装

```sh
adb connect TV_IP:5555
python3 scripts/setup.py doctor --serial TV_IP:5555 --bundle .
python3 scripts/setup.py install --serial TV_IP:5555 --bundle .
```

检测为只读，列出设备型号/API/ABI/当前HOME/包状态。安装核对 SHA256SUMS，只安装配套 HOME 和 overlay，尝试授予悬浮窗、使用情况访问及电池优化白名单。受限固件上需自行在电视设置授权。安装过程不自动接管首页、不重启、不清除账号。未激活时会暂时关闭新版开机接收器，避免配置后重启与旧版同时启动；已激活设备的升级不改变它。

配套应用按需选择：

```sh
python3 scripts/setup.py install --serial TV_IP:5555 --bundle . --companions tizentube flclash mytv
```

向导按电视系统 ABI 选 ARM32 或 ARM64，不能仅凭处理器名称判断。已经安装的第三方包直接跳过，避免签名或版本覆盖。x86 或其他未提供架构需自行从上游选择。安装包合集不含频道订阅和代理线路。

若签名冲突，立即停止：不能用卸载解决正在使用的账号问题。v0.2 新包名为 `local.simple.tvhome.dock` / `local.simple.tvoverlay.dock`，旧版可保留。使用自己签名的 `.dock` 版本也需要同一密钥才能更新。

## 3. 完成一次性配置

1. 手动打开 FlClash，导入自己的合法配置，选择能访问目标服务的线路，确认系统 VPN 授权。设置为系统始终开启 VPN（如固件提供）。不要启用不需要的断网封锁。
2. 在 FlClash 开启本地外部控制器（9090），设置非空密钥；按[集成指南](integrations.md)导入轻幕。开启控制器时检查局域网暴露范围和认证，保留认证密钥。
3. 手动打开 TizenTube／其他视频应用，登录并试播。Netflix 从受支持电视的官方商店安装；不要套用别台电视的 Google 服务修复。
4. 如使用家居，按[HA 指南](home-assistant.md)创建专用授权并导入。旧应用的 Android Keystore 凭据不能复制到新包名。
5. 选择开机应用、是否开机启动及状态条显示。默认跟随上次。

## 4. 单独激活首页

```sh
python3 scripts/setup.py activate --serial TV_IP:5555 --bundle .
```

向导在本机记录原HOME与旧轻幕包状态，设置并核对新的HOME，再停用旧 `local.simple.tvhome` / `local.simple.tvoverlay`（只停用，不删除）。系统原厂桌面不会被停用。保留状态文件，特别是电脑更换或文件夹移动时。

不要让两套轻幕的启动服务同时运行；只改变默认HOME并不能禁止另一套响应开机。

## 5. 验收

- 按首页键确认覆盖菜单；返回后原视频应用仍在。
- 左右循环导航、上键进入设置／家居、展开不移动底栏。
- 检查 Clash 状态与目标应用的实际播放，不能只看进程存在。
- 家居先验证状态读取；再选择一个你能现场确认的设备操作。
- 方便时自行重启一次，确认VPN、轻幕与上次应用恢复；不要把后台构建通过当成冷启动验收。

## 6. 回退

```sh
python3 scripts/setup.py rollback --serial TV_IP:5555 --bundle .
```

使用激活时的同一 `--state` 文件（不指定时使用向导默认位置），恢复记录的HOME和旧轻幕状态，停用新版服务；不卸载、不清Netflix或其他应用数据。切换原生HOME时若厂商阻止命令，在电视设置手动选择默认桌面。状态文件丢失时先用 doctor 查当前状态，不要猜原桌面。

升级新版可重复 install，沿用签名保留数据。只有安装成功且配置验收后再激活；不需要每次重复第三方客户端安装。
