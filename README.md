# Beep-Velocity

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5--1.21.x-brightgreen)
![Velocity Version](https://img.shields.io/badge/Velocity-3.0+-blue)
![License](https://img.shields.io/badge/License-MIT-orange)

**Beep-Velocity** 是一款专为 Velocity 代理端设计的提及提醒插件。本项目核心逻辑完整移植自[MCDReforged](https://github.com/MCDReforged/MCDReforged)的 **[Beep](https://github.com/TISUnion/Beep)** 插件。

当玩家执行 `/@` 或 `/@@` 时，插件会无视子服间的隔离，直接向目标玩家发送提及提醒。

## ✨ 功能特性

- 完美还原 源`Beep`插件 的`@`逻辑。
- **双级提醒**：
    - `/@ ID`：发送单次`@`音效。
    - `/@@ ID`：发送强提醒。
- **跨服无感**：由于运行在 Velocity 层，提醒不依赖于具体子服插件，支持全服提及。

## 🚀 安装与依赖

### 1. 依赖
- **[PacketEvents](https://github.com/retrooper/packetevents)**。
- **Velocity 3.0.0+**。

### 2. 安装步骤
1. 将所有`前置依赖`放入 `plugins` 目录。
2. 将本插件放入 `plugins` 目录。
3. 启动服务器。