# 构建与发布

核心构建需要JDK17、Android SDK Platform35（或用LIGHTFRAME_ANDROID_JAR选择）、Build Tools35.0.0、Python3、zip。

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
python3 scripts/build-parallel.py
```

发布用 `.dock` pair 输出 `dist/LightframeHome-Parallel.apk` 与 `dist/LightframeOverlay-Parallel.apk`。两个APK必须同签名；默认本地生成 `.local/parallel-signing.jks` 开发密钥，正式维护必须保管原密钥并使用 `LIGHTFRAME_KEYSTORE`、`LIGHTFRAME_KEY_ALIAS`、`LIGHTFRAME_STORE_PASSWORD` 和 `LIGHTFRAME_KEY_PASSWORD` 环境变量。不要把密钥加入Git，也不要把新开发密钥当作能覆盖现有安装的发布密钥。

`sh scripts/build.sh` 仍构建历史原包名，不适用于v0.2新安装向导。历史安装/节目单工具不等于通用部署流程。

可选守护：`sh companion/playback-guardian/build.sh`，密钥在其 `.local/`；升级须沿用原签名。不要把它当成所有设备的必需组件。

## 检查

```sh
python3 -m unittest discover -s tests -p 'test_setup.py' -v
python3 -m py_compile scripts/setup.py scripts/configure.py scripts/build-parallel.py
```

`preview/build.sh` 构建隔离原生预览，不联网、不控制设备、不设HOME。`preview/check_navigation.py`只用于开发者指定的模拟器，不能替代电视验收。README应用栏与面板图片来自该预览；流光图片明确标注为实机设置预览截图。

发布前：校验SDK/API/包名、签名与ABI；生成SHA256SUMS；测试向导的只读检查、错误停止和回退；检查暂存文件、图片与Git差异没有个人配置。配套APK使用上游原版，记录版本/下载URL/哈希，保留源码和许可。发布向导不内置订阅、不包含GMS或从用户电视提取的私有数据。

v0.2.1首次公开并行版保留旧版v0.1.7发行记录。新旧版布局不同，升级文档必须明确家居分组迁移限制。


v0.2.2 核心版本：Home 8.6 (14)、Overlay 8.34-dock (43)、可选 Guardian 2.11 (13)。已安装签名一致、数据保留的实机升级与窗口交接通过；本轮没有再次执行实机冷重启。

发布包由 `scripts/package-release.py` 生成。先把已签名 APK、PROVENANCE.json、third-party-sources.zip 与 licenses/ 放入独立目录，再执行：

```sh
python3 scripts/package-release.py --version v0.2.2 --assets /path/to/release-assets
```

两个 ZIP 包使用同一份八个 APK 的 SHA256SUMS。BUILD-INFO.json 记录源码提交及自有 APK 版本；RELEASE-SHA256SUMS 校验全部发布附件。不要把个人电视状态或截图目录整个加入发布包。
