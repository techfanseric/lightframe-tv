# 网络与配套应用

## FlClash：先配置，再导入控制器凭据

YouTube/Netflix 的启动门控要求：FlClash 已配置、VPN 已连通、本机 `127.0.0.1:9090` 控制器能认证、目标网络可达。未满足时等待并提示，不保证线路本身可用。FlClash 原版配套 APK 没有内置订阅；实际验证设备使用过较早的定制版本，Release 所附上游 0.9.2 仍需自行验收启动行为。

v0.2.3 起，**设置 → FlClash** 提供三档：

- **自动**：原行为，要求本机 VPN 与控制器可用。
- **直连**：局域网路由已经做代理时使用。只探测目标站点是否可达，不要求本机 VPN，也不再在打开应用前把 FlClash 拉到前台。状态条显示设备总流量而不是代理流量。
- **关闭**：完全不探测，直接打开应用。

直连与关闭都不改变已安装的 FlClash、订阅或系统 VPN 授权，只改变轻幕自己的检查与显示方式。

将控制器密钥写入仅自己可读的本地文本文件，**不要写入命令行参数或 Git**：

```sh
python3 scripts/configure.py --serial TV_IP:5555 --clash-secret /private/controller.txt
```

脚本通过受 DUMP 权限保护的固定文件导入器同时配置两个 `.dock` 包，随后删除电视暂存文件。不要把密钥、订阅、配置导出到 issue。应用私有设置不等于硬件保险库；家居 token 另用 Android Keystore 加密。

常驻状态条显示实际节点、延迟、上下行速率。左侧日期时间和右侧网络条共用“设置 → 状态栏”的智能／常开／常关选项。智能模式在 B 站前台隐藏。

## 音响跟随（可选）

电视接外部设备时，音响需要切到它自己的 HDMI 输入；回到电视自带应用时切回电视的 ARC 通道。这两者是音响上的两个不同输入，配置时要分别给出名称：

```sh
python3 scripts/configure.py --serial TV_IP:5555 --bose-host 192.168.1.50 --bose-port 8090 --bose-account TV --bose-hdmi-account HDMI_1
```

`--bose-account` 是电视的 ARC 通道名称，`--bose-hdmi-account` 是音响自身 HDMI 输入的名称。默认即上例。音响不暴露可选输入或接口不兼容时不切换，静音与音量不受影响。

## TizenTube

Release 提供上游 TizenTube Cobalt 2.0.3 的 ARM32、ARM64 原版包。YouTube 入口映射 `io.gh.reisxd.tizentube.cobalt`，辅助功能描述明确为 YouTube · TizenTube。它不是官方 YouTube 客户端。已有安装默认跳过；视频内容和登录由客户端管理。

## IPTV / MyTV

提供 MyTV 2.2.7 通用 APK，入口包 `top.yogiczy.mytv.tv`。使用者自行导入有权使用的节目源；Release 不提供订阅凭据或打包私人播放列表。历史节目单准备工具见 [IPTV](iptv.md)，历史源不代表当前可播放。

## Netflix / 哔哩哔哩

从设备官方商店或服务方官方渠道安装；轻幕 Release 不镜像其 APK、Google 服务或账号数据。Netflix TV 的 DRM、设备认证、地区与订阅均独立于轻幕。仅看到登录页或禁止截图不能证明实际播放成功。
