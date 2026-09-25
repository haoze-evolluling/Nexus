# Settings Page Floating Title Card Design

**Date:** 2026-06-21
**Status:** Approved

## Overview

将设置页面的标题卡片（"设置"）从 RecyclerView 中移出，改为独立悬浮元素，始终固定在页面顶部。返回按钮嵌入标题卡片内部。卡片使用毛玻璃效果，滚动时阴影增强。

## Current State

```
FrameLayout (bg_home_screen)
├── RecyclerView
│   └── [0] TitleHeader ("设置" 72dp 渐变卡片)
│   └── [1] SectionHeader
│   └── ...
└── ImageButton (返回按钮, 独立浮动, 36dp, marginTop=38dp)
```

问题：
- 标题卡片随 RecyclerView 滚动消失
- 返回按钮和标题文字不在同一容器内，位置关系脆弱
- 返回按钮在浅色背景上不可见

## New Design

```
FrameLayout (bg_home_screen)
├── RecyclerView
│   └── [0] SectionHeader (原 TitleHeader 移除)
│   └── ...
└── ConstraintLayout (悬浮标题卡片, elevation=4dp→12dp)
    ├── ImageButton (返回按钮, 36dp, 左侧垂直居中)
    └── TextView "设置" (居中, marginStart=48dp)
```

### 1. 布局结构

| 属性 | 值 |
|------|-----|
| 根布局 | FrameLayout（保持不变） |
| 卡片类型 | ConstraintLayout |
| 卡片高度 | 72dp |
| 卡片 margin | 20dp 四周 |
| 卡片圆角 | 22dp（保持 bg_home_hero 风格） |
| 返回按钮 | 36dp x 36dp，左侧垂直居中 |
| 标题文字 | 居中，marginStart=48dp（为按钮留空间） |
| z 轴层级 | RecyclerView(0) < 卡片(4dp) < 返回按钮(16dp) |

### 2. 毛玻璃效果

**API 31+（Android 12+）：** 真模糊
- `View.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP))`
- 卡片背景 `#88F0F0F5`（半透明），配合模糊呈现毛玻璃质感

**API 28-30（低版本回退）：** 模拟模糊
- 卡片背景使用半透明渐变 `#DDF0F0F5`
- 无性能开销，视觉上接近毛玻璃效果

**实现方式：** Kotlin 代码中根据 `Build.VERSION.SDK_INT` 动态设置，XML 中设置默认半透明背景作为回退。

### 3. 滚动阴影增强

- **初始状态：** elevation = 4dp（轻微阴影）
- **滚动时：** RecyclerView.OnScrollListener 监听滚动距离
- **过渡：** 0dp 滚动 → 40dp 滚动，elevation 从 4dp 线性增长到 12dp
- **动画：** 使用 `ObjectAnimator` 平滑过渡 `translationZ`，避免突兀跳变

### 4. RecyclerView 调整

- 从 `SettingsAdapter` 数据源中移除 `TitleHeader` 类型
- RecyclerView 直接从第一个 `SectionHeader` 开始
- RecyclerView 的 `paddingTop` 设为 `92dp`（20dp margin + 72dp 卡片高度），避免内容被悬浮卡片遮挡

### 5. 返回按钮

- 嵌入标题卡片内部，不再独立浮动
- 背景使用 `bg_settings_back_button`（`#4D000000` 深色半透明）
- 图标 `baseline_arrow_back_24`，tint 白色 `home_on_hero`
- 点击行为不变：`navigateToHome()`

## File Changes

| File | Change |
|------|--------|
| `content_settings.xml` | 移除 RecyclerView 上方浮动返回按钮，添加悬浮卡片 ConstraintLayout（含返回按钮），RecyclerView 添加 paddingTop |
| `item_settings_title_header.xml` | 不再使用（或修改为悬浮卡片布局） |
| `MainActivity.kt` | `setupSettingsPage()` 中：移除 TitleHeader 数据项，添加滚动监听设置阴影，API 31+ 设置 RenderEffect |
| `SettingsItem.kt` | 可选：移除 `TitleHeader` 类型（如仅设置页使用） |

## Testing

- 验证标题卡片始终固定在顶部，不随 RecyclerView 滚动
- 验证返回按钮点击正确导航回首页
- 验证滚动时阴影从 4dp 过渡到 12dp
- 验证 API 31+ 真模糊效果正常
- 验证 API 28-30 回退半透明效果正常
- 验证 RecyclerView 内容不被悬浮卡片遮挡
- 验证暗色模式兼容性