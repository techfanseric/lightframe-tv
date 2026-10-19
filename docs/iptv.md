# IPTV 离线准备与安装

轻幕新增 `IPTV` 入口，对应 [我的电视 tv2.2.7](https://github.com/yaoxieyoulei/mytv-android/releases/tag/tv2.2.7)，包名 `top.yogiczy.mytv.tv`。官方 APK 的最低系统为 API 21，适用于本项目已记录的 Android 9 电视；已在该电视安装并完成节目源导入，用户确认使用效果；这不代表所有频道的播放和解码均已逐项验证。该上游项目目前已归档，安装包保持官方签名，不修改或重签。

菜单、开机指定应用、跟随上次、最近使用排序及当前应用排除都支持第四个应用。IPTV 不走 YouTube / Netflix 的代理等待流程，状态栏保留现有智能显示逻辑。

## 已准备内容

运行 `python3 scripts/prepare-iptv.py` 下载官方 APK，以及 [iptv-org](https://github.com/iptv-org/iptv) 的全球、大陆、香港、台湾和中文公开源。所有 APK、节目源副本、哈希和本地清单保存在忽略目录 `.local/iptv/`，不提交到 Git。

2026-10-19 准备时，全球原始列表包含 11,361 条播放条目。播放器的 M3U 解析器要求 EXTINF 后紧接 URL，且不处理频道专用请求头，因此兼容列表排除带 EXTVLCOPT / KODIPROP 的条目及不支持的协议。默认合并列表按大陆、香港、台湾、中文、全球顺序去重，得到 10,630 个播放地址；原始文件同时保留。此数字表示节目单中的地址数量，不表示已确认可播放的频道数。

源列表来自公开频道集合，不包含运营商账号、付费频道凭据或私人订阅。地域限制、网络条件和源失效都会影响播放。没有对全部地址进行播放验收。EPG 沿用播放器现有配置，本次未验证节目预告匹配。

## 电视开机后

先在项目根目录准备文件（安装已有 Release APK 时不需要重新构建轻幕）：

```sh
python3 scripts/prepare-iptv.py
```

将 Release 的 `LightframeHome.apk` 和 `LightframeOverlay.apk` 放入 `dist/`；或按 README 的构建说明生成它们。然后执行（先保证 ADB 已连接并获授权）：

```sh
./scripts/install-iptv.sh "$TV_ADB_SERIAL"
```

默认只安装三个 APK，不启动应用、不按遥控器键、不改默认桌面、不重启。之后用遥控器打开 IPTV，再执行：

```sh
python3 scripts/configure-iptv.py "$TV_ADB_SERIAL"
```

如果准备好让脚本打开 IPTV 并导入，可明确使用：

```sh
./scripts/install-iptv.sh "$TV_ADB_SERIAL" --launch-and-configure
```

导入脚本通过 ADB 端口转发连接播放器自身的配置接口；将六个兼容节目单存入播放器专属外部文件目录，合并源列表并默认选中“全部公开频道（中文优先）”，读取配置确认后移除转发。现有其他源、收藏、节目预告和显示偏好保留。重复导入同路径源不会新增重复项。首次使用若播放器要求确认使用协议，需在电视上自行完成，再重跑导入。

导入成功后，关闭并重新打开 IPTV，使界面载入新的本地节目单。已在目标电视确认合并列表解析为 178 个分组、10,630 条播放地址。

本地副本可以避免首次载入依赖节目单网站，但播放仍需联网。需要更新源时，重新运行准备脚本和导入脚本。只更新兼容列表；不会清除应用数据。
