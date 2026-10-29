# 第三方资源与配套安装包

Release镜像以下上游未修改的开源APK；SHA256SUMS验证下载完整性，来源清单JSON记录上游资产摘要、URL、包名与ABI。校验值不能代替对发布者的信任。FlClash与MyTV对应源码快照、全部许可证以及TizenTube对应源码下载地址放在 `third-party-sources.zip`，与二进制同时提供。

| 项目 | 版本 | 许可 | 上游 |
| --- | --- | --- | --- |
| TizenTube Cobalt | 2.0.3，ARM32/64 | BSD-3-Clause（仓库；依赖另有许可） | https://github.com/reisxd/TizenTubeCobalt/releases/tag/v2.0.3 |
| FlClash | 0.9.2，ARM32/64 | GPL-3.0，依赖许可见对应源码 | https://github.com/chen08209/FlClash/releases/tag/v0.9.2 |
| MyTV | 2.2.7，通用 | MIT | https://github.com/yaoxieyoulei/mytv-android/releases/tag/tv2.2.7 |
| Phosphor Fill | 2026-10-30资产快照 | MIT | https://github.com/phosphor-icons/core |
| Lucide | 历史资源 | ISC | apps/overlay/third-party/lucide |

源码快照保留上游构建脚本、依赖版本/锁文件及许可；构建所需工具链和依赖按各上游指南获取。没有修改或重新签名配套第三方APK。源码与构建环境不来自用户电视。

Netflix、YouTube、Bilibili图形归各商标权利人，来源在 `docs/assets/brand-sources.md`。YouTube入口启动TizenTube，非Google官方客户端；品牌图形不表示官方背书。设置/家居入口图形的生成记录也保存在assets文档。

不提供Netflix、哔哩哔哩、Google服务APK、代理订阅或家庭设备授权。需要这些服务的用户从官方渠道安装并完成自己的授权。
