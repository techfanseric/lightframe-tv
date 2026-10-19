# 智能家居

设置行的“智能家居”使用 Lucide `house-plug` 图标，打开原生悬浮控制面板，不切换正在观看的应用或信号源。入口按“房间 → 设备 → 控制与状态”浏览 HA 完整目录，保留离线、禁用和暂未提供实体的设备；没有房间归属的项目放在“未分配房间”，不同家庭的同名房间按 HA 的区域 ID 分开。实体的房间覆盖优先于设备房间。新增设备自动发现，不再受固定 8 项或 32 项列表限制。

通过“选择常用设备”按房间找到设备，勾选其控制或状态项并保存，才会加入应用行上方的快捷行。更新和目录刷新保留原有选择及顺序，不自动添加新发现的设备。每行最多四项、最多三行；选择数量按屏幕实际可容纳的行数限制，避免已选择的项目被藏起来。每次打开切换菜单仍默认聚焦应用行，保留 MRU、当前应用排除和每行左右循环。

支持空调开关和目标温度、窗帘开关和停止、风扇/开关/灯的开关、扫地机器人开始/暂停/回充。按实际可用操作判断：只有开关两项时，按确认键直接切换，包括关闭中或无法提供调温选项的空调；有调温、停止、回充等其他操作时才显示菜单。切换空调前重新读取设备状态，再发送开启或关闭指令；新风、灯和普通开关由 HA 切换。状态读取和成功反馈不能代替真人实控验收。菜单打开时刷新状态，关闭后不持续轮询 HA。

传感器和电视暂未支持操作的实体也显示在对应设备里，并标明“仅查看”；不会把未知实体当成电源开关发送指令。设备页和控制页的返回键逐级返回。状态读取使用 REST API；设备、实体和房间登记通过同一非管理员账号的 WebSocket API 读取。连接启动时后台同步一次，进入家居入口或按“刷新”重新同步；切换菜单可见期间每 5 秒刷新状态，登记信息最多每分钟更新一次。登记失败保留应用私有目录中的上次目录并提示，不能当成完整同步成功。

## 连接

建议使用独立的非管理员 HA 用户，为其生成专用访问 token。电视通过 HA REST API 读取状态和调用固定的设备服务，不需要 HA 管理权限，不保存路由器密码。

在 Git 之外准备 JSON，或将它保存到已忽略的 `.local/` 中：

```json
{
  "url": "http://your-ha-host:8123",
  "token": "your-device-specific-access-token",
  "controls": [
    {"entity_id": "climate.bedroom", "name": "主卧空调"},
    {"entity_id": "cover.west", "name": "西窗帘"}
  ]
}
```

```sh
python3 scripts/configure-home-assistant.py "$TV_ADB_SERIAL" /private/path/home-assistant.json
```

`controls` 作为原有设备别名保留，可用空数组交由 HA 自动发现完整目录。新连接默认不预选快捷项，升级已有连接保留其快捷项。导入广播仅允许持有系统 DUMP 权限的调用方；固定导入文件处理后删除，token 使用 Android Keystore AES-GCM 加密保存到应用私有目录，备份关闭。URL 不跟随重定向，连接失败不尝试控制设备。HTTP 局域网连接仍是明文传输，已有 HTTPS 地址可直接使用且保留证书和主机名校验。

温度从 HA 的实例单位和实体单位读取，统一显示摄氏度；调温指令按 HA 使用的单位和设备步长换算，不修改 HA 的全局单位设置。

家居控制未注册全局语音按键服务。遥控器真人采音、中文识别服务和 Assist 语音管线须单独验证，不能把控制面板接通当作语音已完成。DUMP 权限保护的 `PROBE_REMOTE_VOICE` / `REMOTE_VOICE_RESULT` 广播提供手动的 20 秒采音诊断；须先授权麦克风，统计采样数、峰值、RMS 和分秒变化，不保存或上传音频、不执行任何识别或设备动作。

## 验证

使用 JDK 17 运行独立逻辑检查：

```sh
mkdir -p .local/smart-home-tests
javac -d .local/smart-home-tests apps/overlay/src/local/simple/tvoverlay/HomeControl.java apps/overlay/src/local/simple/tvoverlay/HomeCatalog.java apps/overlay/src/local/simple/tvoverlay/HomeTemperature.java apps/overlay/src/local/simple/tvoverlay/MenuNavigation.java apps/overlay/src/local/simple/tvoverlay/HomeSocket.java tests/SmartHomeLogicTest.java tests/HomeSocketTest.java
java -cp .local/smart-home-tests local.simple.tvoverlay.SmartHomeLogicTest
java -cp .local/smart-home-tests local.simple.tvoverlay.HomeSocketTest
```

再按仓库说明运行 `scripts/build.sh`。实机检查应用行默认焦点、上下行切换、首尾循环、返回关闭、当前输入保留、网络失败反馈；实际设备动作应在用户方便时验证。
