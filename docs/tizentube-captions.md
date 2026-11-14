# TizenTube 字幕语言

TizenTube（`io.gh.reisxd.tizentube.cobalt`）是 YouTube 的 Cobalt 发行版，播放器完全由
YouTube TV 的网页播放器承载。下面是实测得到的字幕语言机制，供后续排查使用。

## 界面语言与字幕语言是两件事

界面语言由 YouTube 的 `PREF` cookie 决定，设备上当前是 `PREF hl=en`，所以界面是英文。
字幕语言不由这个 cookie 决定，而是由播放器的选轨逻辑决定。

## 自动选轨的优先级

播放器在每次打开视频时按以下顺序挑选字幕轨道，命中即用：

1. `yt-player-caption-sticky-language` — 上一次手动选择的语言
2. `captionsLanguagePreference` — 来自 InnerTube `/player` 响应的 `cc_lang_pref`
3. 播放器自身的 `captionsLanguagePreference`
4. 视频的 `yt:cc_default_lang` 字段
5. 视频自带 `isDefault` 的轨道，或列表里的第一条

语言码先做全等比较，再退回到只比较基础语言，所以 `zh-HK` 能命中 `zh-Hans` 的轨道。

## 手动选择一次即可长期生效

在播放器里手动选一次字幕语言，播放器会把它追加进
`yt-player-caption-language-preferences` 数组（保存在 localStorage），之后每个视频都会
优先使用这个语言，不需要逐个视频再选。

## 存储位置

Cobalt 把 localStorage 存在 LevelDB：

```
/data/data/io.gh.reisxd.tizentube.cobalt/app_content_shell/content_shell/Local Storage/leveldb
```

包是 `DEBUGGABLE` 的，adb shell 可以用 `run-as` 读取：

```sh
adb shell "run-as io.gh.reisxd.tizentube.cobalt ls \
  '/data/data/io.gh.reisxd.tizentube.cobalt/app_content_shell/content_shell/Local Storage/leveldb'"
```

已知的键：

| 键 | 含义 |
| --- | --- |
| `yt-html5-player-modules::subtitlesModuleData` | 字幕模块数据，含字幕开关 |
| `yt-player-caption-language-preferences` | 手动选过的字幕语言数组 |
| `yt-player-caption-sticky-language` | 跨视频保持的字幕语言 |
| `yt-player-user-settings` | 播放器用户设置 |
| `yt-player-caption-display-settings` | 字幕外观 |

写入这些键时 LevelDB 正被应用占用，直接改文件有损坏风险。

## 已知限制

- 「粘性语言」依赖实验开关 `web_enable_caption_language_preference_stickiness`，
  该开关不在当前下发的 262 个实验标记里，所以第 1 优先级可能不生效。
- 第 2 优先级 `cc_lang_pref` 来自服务端，与账号的字幕设置有关；设备上的
  YouTube 会话没有登录（cookie 里没有 `SID`/`SSID`/`APISID`），所以账号级的
  字幕偏好不可用。
- 因此在没有登录、实验开关也未开启的情况下，可靠的做法是在播放器里手动选一次
  字幕语言，让它写进 `yt-player-caption-language-preferences`。

## 复核方法

```sh
# 看当前有哪些播放器偏好
adb shell "run-as io.gh.reisxd.tizentube.cobalt sh -c \
  'cat /data/data/io.gh.reisxd.tizentube.cobalt/app_content_shell/content_shell/Local Storage/leveldb/*'" \
  | strings | grep -oE 'yt-player-[a-z-]+' | sort -u
```

