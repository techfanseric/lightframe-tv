# 本地集成配置 / Local integrations

## Clash 状态条

需要电视上的 FlClash 和已启用的本机 HTTP 控制器 `127.0.0.1:9090`。节点、延迟、速率由控制器实际状态读取；这不是订阅管理界面。

将控制器凭据保存在仓库之外的本地文件，再执行：

```sh
./scripts/configure-clash.py "$TV_ADB_SERIAL" /private/path/controller-credential
```

配置由受 Android DUMP 权限保护的 receiver 写入 HOME 应用私有偏好，再通过签名保护的两包通信传递给贴片。不要把订阅、控制器凭据或电视偏好导出到仓库。

## 可选广告声音控制

此功能依赖独立 LAN 广告助手，本仓库只包含客户端。当前端点为 `http://10.0.0.1:17792/v1/ad-mute`；路由不同须调整 `AdMuteClient.java` 中的地址再构建。

- `Authorization: Bearer <narrow-capability-token>`，只授权广告静音开关，不是路由管理员凭据。
- `GET` 读取状态。
- `POST` JSON `{"mute_ads": false}` 或 `{"mute_ads": true}`，不修改 `skip_ads`。
- 成功返回 `{"version":1,"mute_ads":false,"skip_ads":true,"connected":true,"applied":true}`。
- 默认 `mute_ads=false`。`connected/applied=false` 表示偏好已保存、等待 YouTube 连接；网络失败显示不可用并保留上次确认值，不把点击当成功。

从助手取得 token 后保存在仓库之外，执行：

```sh
./scripts/configure-ad-mute.sh "$TV_ADB_SERIAL" /private/path/ad-mute-capability
```

下一次 YouTube HOME 菜单请求会将文件导入应用私有偏好，并删除 external-files 临时文件。token 不嵌入 APK，不写入源码，不需要 Google 账号或路由 root 密码。客户端只在打开菜单或主动操作时单次请求，连接超时 1.5 秒、读取超时 3 秒，不循环重试。

现有部署中，助手只应解除自己施加的静音，不覆盖用户手动静音；这属于后端职责。自动跳过与声音开关独立。该功能不等同于消除广告。
