package com.haoze.claudekeyboard.bluetooth

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

/**
 * 本设备对外呈现的 HID 身份。
 *
 * 一个 [BluetoothHidDevice] 应用只能在注册时确定一份 SDP 设置 + 报告描述符，
 * 想换身份必须先 unregisterApp 再 registerApp，主机侧通常需要重新连接一次
 * （多数主机重新枚举 Report Map 后即可生效，少数需要删除配对再配一次）。
 *
 * 两个 Profile 的键盘 / 鼠标 / 消费者控制报告结构完全相同（Report ID 1 / 2 / 3，
 * 字节布局一字未动），区别只有三点：
 *   1. 报告描述符里 TLC 的顺序（Gamepad 排在首位还是末位）
 *   2. Gamepad TLC 的轴布局（手柄模式补齐 Z / Rz 主轴与 Rx / Ry 扳机轴）
 *   3. SDP 的 subclass1 与设备名称
 *
 * 因此切换 Profile 不会破坏任何现有键鼠功能。
 */
enum class HidProfile(val storageKey: String) {

    /** 键鼠组合设备：主机显示为键盘 + 触控板，适合办公 / 文字输入。 */
    KEYBOARD_MOUSE("keyboard_mouse"),

    /** 标准游戏控制器：主机显示为游戏手柄，适合 Android TV / PC / 云游戏。 */
    GAMEPAD("gamepad");

    /** 构造注册用的 SDP 设置。 */
    fun buildSdpSettings(appName: String): BluetoothHidDeviceAppSdpSettings = when (this) {
        KEYBOARD_MOUSE -> BluetoothHidDeviceAppSdpSettings(
            "$appName Combo",
            "$appName Bluetooth HID Combo",
            appName,
            // 键鼠组合，主机据此启用 boot 协议键盘并显示为键鼠设备
            BluetoothHidDevice.SUBCLASS1_COMBO,
            DescriptorCollection.KEYBOARD_MOUSE
        )

        GAMEPAD -> BluetoothHidDeviceAppSdpSettings(
            // 部分主机（Windows 蓝牙面板、Android TV 配对列表）直接展示 SDP name
            "$appName Gamepad",
            "$appName Bluetooth HID Game Controller",
            appName,
            // 关键：不能再用 SUBCLASS1_COMBO。那等于在 SDP 里声明
            // "我是键盘 + 指向设备组合"，主机据此把设备登记为键盘，
            // 游戏的手柄枚举器直接跳过，表现就是"连上了但被识别成键盘"。
            // Android SDK 只公开 NONE / KEYBOARD / MOUSE / COMBO 常量，没有 gamepad，
            // 这里直接用 Bluetooth 规范 Peripheral 大类下的 Gamepad 值（0x08，与 Xbox /
            // 8BitDo 等真手柄一致），主机在蓝牙面板把它显示为手柄，
            // Report Map 里首位的 Usage(Gamepad) TLC 则让游戏把它枚举成手柄。
            SUBCLASS_GAMEPAD,
            DescriptorCollection.GAMEPAD
        )
    }

    companion object {
        /**
         * 默认值：键鼠组合。连接后主机把本设备登记为键盘 + 触控板组合设备，
         * 适合日常办公与文字输入；手柄功能仍保留在同一份报告描述符中。
         * 需要标准手柄识别的游戏主机可在"设置 → 连接设置 → 设备类型"切换为游戏手柄。
         */
        val DEFAULT: HidProfile = KEYBOARD_MOUSE

        /**
         * Bluetooth CoD minor device class（Peripheral 大类下）：
         * 0x04 = Joystick，0x08 = Gamepad。SDK 未提供 gamepad 常量，按规范写死。
         */
        private const val SUBCLASS_GAMEPAD: Byte = 0x08

        fun fromStorageKey(key: String?): HidProfile =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
