修复开机或 CEC 重新发现已有 HDMI 设备时，轻幕误触发信源切换的问题。

- 原逻辑把“发现已有输入”当成“新插入设备”，主动打开 HDMI 播放器；实机启动日志已确认调用来自轻幕。
- 新增输入只建立状态基线，自动切换仍要求观察到断开到连接的变化；保留手动信源选择。
- 保留电视现有 HDMI-CEC、ARC、音响联动和应用偏好。

Home **8.4 / code 12**；Overlay **8.23 / code 32**。两个 APK 沿用既有共享签名，包名和签名权限保持不变，可覆盖更新并保留设置。请配套安装，使用 `SHA256SUMS.txt` 核对文件。

已完成 JDK 17 / SDK 35 构建、APK 签名及已安装版本证书比对。Overlay 已在电视后台覆盖安装，前台 TizenTube 未切换，贴片服务与 HDMI 回调恢复正常，未发现新进程崩溃。未执行重启或待机操作，待机唤醒与冷启动仍待用户实际开机验收。

私密配置、设备日志、设备偏好、第三方播放器和节目源快照不随 Release 分发。

Fixes unintended HDMI player launches when startup or CEC rediscovery reports an already-connected input. Input registration now only establishes a baseline, while observed disconnected-to-connected transitions retain automatic switching. Includes both signed APKs and SHA-256 checksums. Background installation and service restoration were verified without changing the foreground TizenTube app; standby wake and cold boot validation remain pending.
