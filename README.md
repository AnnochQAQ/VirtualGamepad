# VirtualGamepad

把一台 Android 设备当作「手柄」，通过 ADB + UHID 控制另一台 Android 设备。

基于 [ScrcpyForAndroid](https://github.com/Miuzarte/ScrcpyForAndroid)（作者 Miuzarte）修改，后者又是
[scrcpy](https://github.com/Genymobile/scrcpy)（作者 Genymobile）的 Android 客户端。

## 说明

- 保留了 ADB 连接、UHID 虚拟手柄两条链路
- 移除了投屏、音频、终端、文件管理等与本项目目标无关的功能
- 支持虚拟手柄（屏幕上的按键 / 摇杆）、物理手柄转发、布局编辑与导入导出

## 用到的项目

| 项目 | 说明 |
|---|---|
| [scrcpy](https://github.com/Genymobile/scrcpy) | 原始项目 |
| [ScrcpyForAndroid](https://github.com/Miuzarte/ScrcpyForAndroid) | 本项目的基础 |
| [miuix](https://github.com/compose-miuix-ui/miuix) | UI 组件 |
| [BoringSSL (vvb2060)](https://github.com/vvb2060/ndk.boringssl) | 加密 |
| [libcxx (LSPosed)](https://github.com/LSPosed/libcxx) | C++ 标准库 |
| [Bouncy Castle](https://www.bouncycastle.org/) | 加密 |
| [Conscrypt](https://github.com/google/conscrypt) | TLS |
| [Reorderable](https://github.com/Calvin-LL/Reorderable) | 列表拖拽 |
| [TinyPinyin](https://github.com/promeg/TinyPinyin) | 拼音排序 |
| AndroidX / Jetpack Compose | UI 框架 |
| Kotlin | 语言 |

## 版权

本项目基于以下开源项目修改，遵循 **Apache License 2.0**：

- [scrcpy](https://github.com/Genymobile/scrcpy) — Copyright (C) Genymobile
- [ScrcpyForAndroid](https://github.com/Miuzarte/ScrcpyForAndroid) — Copyright (C) Miuzarte

根据 Apache License 2.0，你可以在遵守许可证条款的前提下使用、修改、分发本项目。完整条款见 [LICENSE](LICENSE)。

## License

[Apache License 2.0](LICENSE)
