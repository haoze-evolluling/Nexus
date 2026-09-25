package com.haoze.nexus.bluetooth

/**
 * HID 报告描述符集合。
 *
 * 主机（Windows / Android TV / 游戏机 / 浏览器 Gamepad API）判定"这是什么设备"时，
 * 除了 SDP 里的 HID DeviceSubclass 之外，主要依据 Report Map 中**第一个顶层应用集合（TLC）**
 * 的 Usage：
 *   - Usage(Keyboard) → 归类为键盘
 *   - Usage(Gamepad)  → 归类为游戏控制器
 *
 * 早期版本把 Gamepad TLC 排在键盘/鼠标/消费者控制之后，主机枚举到的第一个 TLC 是键盘，
 * 于是整个设备被登记成键盘，游戏的手柄枚举器直接跳过，表现为"连上了但被识别为键盘"。
 *
 * 因此这里提供两套描述符，由 [HidProfile] 选择：
 *   - [KEYBOARD_MOUSE]：键盘 TLC 在首位，Gamepad TLC 在末尾 —— 兼容旧行为，
 *     主机显示为键鼠组合设备，手柄输入对宽容的主机（如 Android TV）仍可用。
 *   - [GAMEPAD]：Gamepad TLC 在首位，配合 SDP 名称与非键盘的 subclass1，
 *     让主机把本设备注册为标准 HID 游戏控制器。键盘/鼠标/消费者控制 TLC 全部保留，
 *     手柄模式下键鼠功能不受任何影响。
 */
object DescriptorCollection {

    // ---- 键盘 TLC（Report ID 1）----

    private val KEYBOARD_TLC = descriptor(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x06,                    // Usage (Keyboard)
        0xA1, 0x01,                    // Collection (Application)
        0x85, 0x01,                    //     Report ID (1)

        0x05, 0x07,                    //     Usage Page (Key Codes)
        0x19, 0xE0,                    //     Usage Minimum (224)
        0x29, 0xE7,                    //     Usage Maximum (231)
        0x15, 0x00,                    //     Logical Minimum (0)
        0x25, 0x01,                    //     Logical Maximum (1)
        0x75, 0x01,                    //     Report Size (1)
        0x95, 0x08,                    //     Report Count (8)
        0x81, 0x02,                    //     Input (Data, Variable, Absolute) - 修饰键位图

        0x95, 0x01,                    //     Report Count (1)
        0x75, 0x08,                    //     Report Size (8)
        0x81, 0x01,                    //     Input (Constant) - 保留字节

        0x95, 0x05,                    //     Report Count (5)
        0x75, 0x01,                    //     Report Size (1)
        0x05, 0x08,                    //     Usage Page (LEDs)
        0x19, 0x01,                    //     Usage Minimum (1)
        0x29, 0x05,                    //     Usage Maximum (5)
        0x91, 0x02,                    //     Output (Data, Variable, Absolute) - LED 报告
        0x95, 0x01,                    //     Report Count (1)
        0x75, 0x03,                    //     Report Size (3)
        0x91, 0x01,                    //     Output (Constant) - LED 报告填充位

        0x95, 0x06,                    //     Report Count (6)
        0x75, 0x08,                    //     Report Size (8)
        0x15, 0x00,                    //     Logical Minimum (0)
        0x25, 0x65,                    //     Logical Maximum (101)
        0x05, 0x07,                    //     Usage Page (Key Codes)
        0x19, 0x00,                    //     Usage Minimum (0)
        0x29, 0x65,                    //     Usage Maximum (101)
        0x81, 0x00,                    //     Input (Data, Array) - 6 键滚降数组
        0xC0                           // End Collection
    )

    // ---- 鼠标 TLC（Report ID 2）----

    private val MOUSE_TLC = descriptor(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x02,                    // Usage (Mouse)
        0xA1, 0x01,                    // Collection (Application)
        0x85, 0x02,                    //     Report ID (2)
        0x09, 0x01,                    //     Usage (Pointer)
        0xA1, 0x00,                    //     Collection (Physical)

        // 左/中/右三键
        0x05, 0x09,                    //         Usage Page (Button)
        0x19, 0x01,                    //         Usage Minimum (1)
        0x29, 0x03,                    //         Usage Maximum (3)
        0x15, 0x00,                    //         Logical Minimum (0)
        0x25, 0x01,                    //         Logical Maximum (1)
        0x75, 0x01,                    //         Report Size (1)
        0x95, 0x03,                    //         Report Count (3)
        0x81, 0x02,                    //         Input (Data, Variable, Absolute)

        // 5 位填充，凑满 1 字节
        0x75, 0x05,                    //         Report Size (5)
        0x95, 0x01,                    //         Report Count (1)
        0x81, 0x01,                    //         Input (Constant)

        // X / Y 位移
        0x05, 0x01,                    //         Usage Page (Generic Desktop)
        0x09, 0x30,                    //         Usage (X)
        0x09, 0x31,                    //         Usage (Y)
        0x15, 0x81,                    //         Logical Minimum (-127)
        0x25, 0x7F,                    //         Logical Maximum (127)
        0x75, 0x08,                    //         Report Size (8)
        0x95, 0x02,                    //         Report Count (2)
        0x81, 0x06,                    //         Input (Data, Variable, Relative)

        // 垂直滚轮
        0x09, 0x38,                    //         Usage (Wheel)
        0x15, 0x81,                    //         Logical Minimum (-127)
        0x25, 0x7F,                    //         Logical Maximum (127)
        0x75, 0x08,                    //         Report Size (8)
        0x95, 0x01,                    //         Report Count (1)
        0x81, 0x06,                    //         Input (Data, Variable, Relative)

        // AC Pan（水平滚动）
        0x05, 0x0C,                    //         Usage Page (Consumer)
        0x0A, 0x38, 0x02,              //         Usage (AC Pan)
        0x15, 0x81,                    //         Logical Minimum (-127)
        0x25, 0x7F,                    //         Logical Maximum (127)
        0x75, 0x08,                    //         Report Size (8)
        0x95, 0x01,                    //         Report Count (1)
        0x81, 0x06,                    //         Input (Data, Variable, Relative)

        0xC0,                          //     End Collection (Physical)
        0xC0                           // End Collection
    )

    // ---- 消费者控制 TLC（Report ID 3）----

    private val CONSUMER_TLC = descriptor(
        // 与主流 Android TV / Google TV 遥控器描述符保持一致
        0x05, 0x0C,                    // Usage Page (Consumer)
        0x09, 0x01,                    // Usage (Consumer Control)
        0xA1, 0x01,                    // Collection (Application)
        0x85, 0x03,                    //     Report ID (3)

        0x15, 0x00,                    //     Logical Minimum (0)
        0x26, 0xFF, 0x03,              //     Logical Maximum (0x3FF)
        0x19, 0x00,                    //     Usage Minimum (0)
        0x2A, 0xFF, 0x03,              //     Usage Maximum (0x3FF)
        0x75, 0x10,                    //     Report Size (16)
        0x95, 0x01,                    //     Report Count (1)
        0x81, 0x00,                    //     Input (Data, Array, Absolute)

        0xC0                           // End Collection
    )

    // ---- 游戏手柄 TLC（Report ID 4）----
    //
    // 采用与 Xbox 360 / DualShock 4 一致的布局，这是 DirectInput、SDL、
    // 浏览器 Gamepad API 以及绝大多数 PC 游戏唯一可靠的识别方式：
    //   - Usage(Gamepad) 顶层集合
    //   - 16 个数字按键
    //   - Hat Switch（十字键，0..8 + Null State，带物理量 0..315 度）
    //   - 4 个主轴 X / Y（左摇杆）、Z / Rz（右摇杆）
    //   - 2 个模拟扳机 Rx / Ry（LT / RT）
    //
    // 旧版用 X / Y / Rx / Ry 表示双摇杆。DirectInput 与主流游戏只读 X/Y/Z/Rz，
    // Rx/Ry 被当成第 5/6 轴而忽略，直接导致"摇杆没反应 / 不被当成完整手柄"。
    private val GAMEPAD_TLC = descriptor(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x05,                    // Usage (Gamepad)
        0xA1, 0x01,                    // Collection (Application)
        0x85, 0x04,                    //     Report ID (4)

        // 16 个按键（2 字节）
        0x05, 0x09,                    //     Usage Page (Button)
        0x19, 0x01,                    //         Usage Minimum (Button 1)
        0x29, 0x10,                    //         Usage Maximum (Button 16)
        0x15, 0x00,                    //         Logical Minimum (0)
        0x25, 0x01,                    //         Logical Maximum (1)
        0x75, 0x01,                    //         Report Size (1)
        0x95, 0x10,                    //         Report Count (16)
        0x81, 0x02,                    //         Input (Data, Variable, Absolute)

        // Hat Switch / 十字键（4 位有效 + 4 位填充 = 1 字节）
        0x05, 0x01,                    //     Usage Page (Generic Desktop)
        0x09, 0x39,                    //         Usage (Hat switch)
        0x15, 0x01,                    //         Logical Minimum (1)
        0x25, 0x08,                    //         Logical Maximum (8)
        0x35, 0x00,                    //         Physical Minimum (0)
        0x46, 0x3B, 0x01,              //         Physical Maximum (315)
        0x65, 0x14,                    //         Unit (Eng Rot: Degree)
        0x75, 0x04,                    //         Report Size (4)
        0x95, 0x01,                    //         Report Count (1)
        0x81, 0x42,                    //         Input (Data, Variable, Absolute, Null State)
        0x65, 0x00,                    //         Unit (None) - 复位，避免影响后面的轴
        0x75, 0x04,                    //         Report Size (4)
        0x95, 0x01,                    //         Report Count (1)
        0x81, 0x01,                    //         Input (Constant) - 填充位

        // 4 个主轴：X / Y = 左摇杆，Z / Rz = 右摇杆（8 字节）
        0x05, 0x01,                    //     Usage Page (Generic Desktop)
        0x09, 0x30,                    //         Usage (X)  - 左摇杆 X
        0x09, 0x31,                    //         Usage (Y)  - 左摇杆 Y
        0x09, 0x32,                    //         Usage (Z)  - 右摇杆 X
        0x09, 0x35,                    //         Usage (Rz) - 右摇杆 Y
        0x15, 0x00,                    //         Logical Minimum (0)
        0x27, 0xFF, 0xFF, 0x00, 0x00,  //         Logical Maximum (65535)
        0x75, 0x10,                    //         Report Size (16)
        0x95, 0x04,                    //         Report Count (4)
        0x81, 0x02,                    //         Input (Data, Variable, Absolute)

        // 2 个模拟扳机：Rx = LT，Ry = RT（4 字节）
        0x09, 0x33,                    //         Usage (Rx) - 左扳机
        0x09, 0x34,                    //         Usage (Ry) - 右扳机
        0x15, 0x00,                    //         Logical Minimum (0)
        0x27, 0xFF, 0xFF, 0x00, 0x00,  //         Logical Maximum (65535)
        0x75, 0x10,                    //         Report Size (16)
        0x95, 0x02,                    //         Report Count (2)
        0x81, 0x02,                    //         Input (Data, Variable, Absolute)

        0xC0                           // End Collection
    )

    /** 键鼠组合描述符：键盘 TLC 在首位，主机登记为键鼠组合设备。 */
    val KEYBOARD_MOUSE: ByteArray =
        KEYBOARD_TLC + MOUSE_TLC + CONSUMER_TLC + GAMEPAD_TLC

    /** 游戏手柄描述符：Gamepad TLC 在首位，主机登记为标准 HID 游戏控制器。 */
    val GAMEPAD: ByteArray =
        GAMEPAD_TLC + KEYBOARD_TLC + MOUSE_TLC + CONSUMER_TLC

    /** 按 [HidProfile] 取对应描述符。 */
    fun forProfile(profile: HidProfile): ByteArray = when (profile) {
        HidProfile.KEYBOARD_MOUSE -> KEYBOARD_MOUSE
        HidProfile.GAMEPAD -> GAMEPAD
    }

    /**
     * 把十六进制项序列转成描述符字节数组。比手写 `0x05.toByte()` 更不易出错。
     */
    private fun descriptor(vararg items: Int): ByteArray =
        ByteArray(items.size) { i -> items[i].toByte() }
}
