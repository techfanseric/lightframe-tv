# 本地集成配置 / Local integrations

## Clash 状态条

需要电视上的 FlClash 和已启用的本机 HTTP 控制器 `127.0.0.1:9090`。节点、延迟、速率由控制器实际状态读取；这不是订阅管理界面。

将控制器凭据保存在仓库之外的本地文件，再执行：

```sh
./scripts/configure-clash.py "$TV_ADB_SERIAL" /private/path/controller-credential
```

配置由受 Android DUMP 权限保护的 receiver 写入 HOME 应用私有偏好，再通过签名保护的两包通信传递给贴片。不要把订阅、控制器凭据或电视偏好导出到仓库。

## YouTube / TizenTube Cobalt

v0.1.1 的 YouTube 逻辑入口只映射到 `io.gh.reisxd.tizentube.cobalt`；显示名称和保存的 `youtube` / 跟随上次偏好保持兼容。默认启动、手动切换、前台识别和当前应用排除统一使用该映射。

原 `com.amazon.firetv.youtube` 可以继续安装，但需要手动打开，不是轻幕的自动回退目标。轻幕不打包或自动安装第三方视频客户端。

## 历史广告声音控制

早期版本曾提供独立 LAN 助手的广告声音开关。v0.1.1 已移除这项菜单及其 GET/POST 调用，不需要为当前菜单配置此接口。历史客户端和配置脚本暂留源码中，不代表当前菜单仍启用该功能。

此前的路由助手设置不会因移除菜单而被轻幕修改；如仍使用独立助手，应单独管理它。不要把路由管理员凭据、接口 token 或配对数据提交到仓库。
