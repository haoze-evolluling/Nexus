# 设置界面 UI 设计规范与迁移说明

本文档描述 DNSSR 当前设置体系的一级界面、二级界面 UI 规范、设计思路，以及对应 Jetpack Compose 代码实现方式。目标是让这套设置 UI 能较低成本迁移到另一个 Android Compose 项目。

## 1. 适用范围

这套 UI 主要覆盖两类页面：

- 一级设置入口页：展示设置分类和当前摘要，例如“本地 DNS 缓存”“DoH 服务商”“竞速服务商”等。
- 二级设置详情页：承载实际配置、列表管理、单选、多选、开关、输入框、危险操作和加载状态。

当前代码位置：

- 统一设置组件：`app/src/main/java/com/haoze/dnssr/ui/components/SettingsComponents.kt`
- 一级设置页：`app/src/main/java/com/haoze/dnssr/ui/SettingsScreen.kt`
- 导航注册：`app/src/main/java/com/haoze/dnssr/ui/AppNavHost.kt`
- 主题入口：`app/src/main/java/com/haoze/dnssr/ui/theme/Theme.kt`
- 设置持久化：`app/src/main/java/com/haoze/dnssr/ui/AppSettings.kt`

代表性二级页：

- `CacheSettingsScreen.kt`：开关 + 动态展开单选 + 危险操作。
- `LogRetentionSettingsScreen.kt`：简单单选。
- `ProviderManagementScreen.kt`：分组列表 + 顶部操作 + 弹窗编辑。
- `RaceModeSettingsScreen.kt`：多选、单选、按钮、输入框、加载态。
- `RuleManagementScreen.kt`：输入框 + 操作项 + 导航项。
- `DataCleanupScreen.kt`：危险操作 + 二次确认弹窗。

## 2. 总体设计思路

这套设置 UI 的核心思路是“系统设置页风格 + Material 3 实现”：

- 页面只保留一个清晰任务，不做装饰性内容。
- 一级页作为目录和状态概览，二级页负责修改具体设置。
- 相同布局抽成组件，页面只描述业务结构。
- 每个分组使用圆角卡片承载，组外使用标题和说明文字建立层次。
- 列表项统一使用左侧主文案、可选副文案、右侧状态或控件。
- 交互尽量整行可点击，降低触控成本。
- 使用 Material 3 颜色和动态色，自动适配亮色、暗色和 Android 12+ 动态取色。

从迁移角度看，最重要的是先迁移 `SettingsComponents.kt`，然后让新项目的设置页面全部使用这些组件组合，而不是在每个页面重新写 Row、Card、Divider 和 TopAppBar。

## 3. 视觉规范

### 3.1 页面结构

所有设置类页面使用统一结构：

```text
SettingsScaffold
└── TopAppBar
    ├── 返回按钮
    ├── 页面标题
    └── 可选 actions
└── 内容区
    ├── SettingsGroupTitle
    ├── SettingsGroup
    │   ├── SettingsItem / SettingsNavigationItem / SettingsSwitchItem / ...
    │   ├── SettingsDivider
    │   └── SettingsItem / ...
    └── SettingsInfoText
```

一级页和二级页都使用同一个 `SettingsScaffold`，区别在于内容区的组织方式：

- 一级页：多个分组，每个分组通常是导航项，用 `value` 显示当前状态摘要。
- 二级页：一个或多个具体配置分组，包含开关、单选、复选、输入框、按钮、危险操作等。

### 3.2 间距

现有实现中的关键间距：

- 页面内容紧贴 `Scaffold` 的 `innerPadding`，避免和状态栏、TopAppBar 重叠。
- 分组卡片水平外边距：`16.dp`。
- 分组标题水平内边距：左右 `32.dp`，顶部 `24.dp`，底部 `8.dp`。
- 说明文字水平内边距：左右 `32.dp`，顶部 `4.dp`，底部 `8.dp`。
- 列表项内边距：水平 `16.dp`，垂直 `12.dp`。
- 列表项最小高度：`44.dp`。
- 列表项内部横向间距：`12.dp`。
- 标题与副标题垂直间距：`2.dp`。

这些数值让分组边缘、标题文本和列表项内容形成对齐关系：

- 卡片外边距是 `16.dp`。
- 卡片内列表项再有 `16.dp` 内边距。
- 因此标题和说明文字使用 `32.dp`，与列表项正文左边缘对齐。

迁移时建议保留这套对齐规则。如果改变卡片外边距或列表项内边距，需要同步调整 `SettingsGroupTitle` 和 `SettingsInfoText` 的左右边距。

### 3.3 圆角与容器

统一圆角定义为：

```kotlin
val SettingsCornerShape = RoundedCornerShape(12.dp)
```

使用位置：

- `SettingsGroup` 的卡片圆角。
- `OutlinedTextField` 的形状。
- 部分日志、缓存、订阅页面中复用的卡片形状。

分组容器使用 `Card`：

```kotlin
Card(
    modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    shape = SettingsCornerShape,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
)
```

这里使用 `surfaceContainer` 而不是写死白色，因此在暗色模式和动态色下会自然适配。

### 3.4 字体层级

当前主要使用 Material 3 typography：

- TopAppBar 标题：`TopAppBar` 默认标题样式。
- 分组标题：`MaterialTheme.typography.titleSmall`。
- 列表项标题：`MaterialTheme.typography.bodyLarge`。
- 列表项副标题：`MaterialTheme.typography.bodySmall`。
- 导航项右侧值：`MaterialTheme.typography.bodyMedium`。
- 说明文字：`MaterialTheme.typography.bodySmall`。

`Type.kt` 只覆盖了 `bodyLarge`，其余字号沿用 Material 3 默认值。迁移时可以直接使用目标项目现有 `MaterialTheme.typography`，只要保留上述语义层级即可。

### 3.5 颜色规范

所有颜色从 `MaterialTheme.colorScheme` 读取：

- 主文字：`onSurface`
- 次级文字：`onSurfaceVariant`
- 卡片背景：`surfaceContainer`
- 分隔线：`outlineVariant`
- 重点动作：`primary`
- 危险动作：`error`
- 禁用文字透明度：`0.38f`

不要在设置组件里写死具体色值。当前项目的主题位于 `Theme.kt`，优先使用 Android 12+ 动态色：

```kotlin
dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
```

迁移到另一个项目时，如果项目已有品牌色，只需要替换 `MaterialTheme` 的 `colorScheme`，设置组件不需要改。

## 4. 一级设置界面规范

一级设置页对应 `SettingsScreen.kt`。它的职责是作为设置目录和状态概览，不直接承载复杂编辑行为。

### 4.1 页面内容

当前一级设置页包含这些分组：

- 解析与缓存
  - 本地 DNS 缓存
- 日志
  - DNS 日志保留
- 解析服务
  - DoH 服务商
  - DNS 查询测速
- 竞速模式
  - 竞速服务商
- 规则与数据
  - 域名屏蔽规则
  - 清理本地数据

每个入口使用 `SettingsNavigationItem`。有当前状态的入口通过 `value` 显示摘要，例如：

- 缓存：`均衡` 或 `未启用`
- 日志保留：`7 天`
- 当前服务商：服务商名称
- 竞速模式：`已启用 · 智慧预测 · 2 个`
- 测速域名：当前域名

这种设计让用户进入二级页前就能知道当前配置，不需要反复点击确认。

### 4.2 一级页布局模板

```kotlin
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToFeatureA: () -> Unit,
    onNavigateToFeatureB: () -> Unit
) {
    val scrollState = rememberScrollState()

    SettingsScaffold(
        title = "设置",
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsGroupTitle("分组标题")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "功能名称",
                    subtitle = "一句话说明功能用途",
                    value = "当前值",
                    onClick = onNavigateToFeatureA
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "另一个功能",
                    subtitle = "一句话说明功能用途",
                    onClick = onNavigateToFeatureB
                )
            }
        }
    }
}
```

### 4.3 一级页设计原则

- 一级页只做导航和摘要，不放复杂表单。
- 分组标题使用短名词，例如“日志”“解析服务”“规则与数据”。
- 列表项标题使用功能名，不使用长句。
- 副标题说明“这个入口能做什么”。
- 右侧 `value` 说明“当前是什么状态”。
- 同一分组内多个条目之间用 `SettingsDivider()`。
- 如果一个分组只有一个条目，不需要分隔线。
- 一级页可以用 `Column + verticalScroll`，因为条目数量固定且较少。

## 5. 二级设置界面规范

二级页是实际操作页面，仍然复用统一的页面骨架和分组组件。

### 5.1 页面类型

当前项目二级页可以归纳为以下类型。

#### 5.1.1 开关 + 展开选项页

代表：`CacheSettingsScreen.kt`

结构：

- 分组标题：“缓存”
- `SettingsSwitchItem` 控制总开关。
- 开启后用 `AnimatedVisibility` 展开多个 `SettingsRadioItem`。
- 下方 `SettingsInfoText` 解释推荐策略。
- “操作”分组放危险操作，如清空缓存。

适用场景：

- 功能有总开关。
- 功能开启后才显示具体策略。
- 关闭时不应展示过多不可用选项。

实现要点：

```kotlin
SettingsSwitchItem(
    title = "本地 DNS 缓存",
    subtitle = if (enabled) "当前策略说明" else "关闭后的影响说明",
    checked = enabled,
    onCheckedChange = ::saveEnabled
)

AnimatedVisibility(
    visible = enabled,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut()
) {
    Column {
        SettingsDivider()
        options.forEachIndexed { index, option ->
            SettingsRadioItem(
                title = option.displayName,
                subtitle = option.summary,
                selected = option == selected,
                onClick = { saveOption(option) }
            )
            if (index < options.lastIndex) SettingsDivider()
        }
    }
}
```

#### 5.1.2 单选配置页

代表：`LogRetentionSettingsScreen.kt`

结构：

- 一个分组标题。
- 一个 `SettingsGroup`。
- 多个 `SettingsRadioItem`。
- 每项之间放 `SettingsDivider()`。
- 底部说明文字解释配置影响。

适用场景：

- 用户只能选择一个枚举值。
- 选项数量较少。

#### 5.1.3 列表管理页

代表：`ProviderManagementScreen.kt`

结构：

- `SettingsScaffold` 的 `actions` 放“新增”按钮。
- 页面顶部用 `SettingsInfoText` 展示当前选中项。
- 用 `LazyColumn` 承载服务商列表。
- 分为“内置”和“自定义”两个分组。
- 每行使用基础 `SettingsItem` 自定义 trailing：选中勾、编辑按钮、删除按钮。
- 新增、编辑、删除使用 `AlertDialog`。

适用场景：

- 列表数据可能增长。
- 行内需要多个操作按钮。
- 需要加载态或 ViewModel 数据。

#### 5.1.4 多选 + 策略页

代表：`RaceModeProviderSettingsScreen`

结构：

- 加载时显示 `SettingsLoadingContent`。
- 数据加载完成后用 `LazyColumn`。
- 服务商选择使用 `SettingsCheckboxItem`。
- 竞速策略使用 `SettingsRadioItem`。
- 最后用按钮执行启用/关闭动作。
- 分组后用 `SettingsInfoText` 给出动态提示。

适用场景：

- 用户需要选择多个对象。
- 配置之间存在启用条件。
- 操作成功后需要回到主界面或触发业务重启。

#### 5.1.5 输入 + 操作页

代表：`RuleManagementScreen.kt`、`RaceModeLatencySettingsScreen`

结构：

- 输入框放在 `SettingsGroup` 内。
- 输入框使用 `OutlinedTextField`，形状使用 `SettingsCornerShape`。
- 输入框后可接 `SettingsDivider()` 和 `SettingsTextItem`。
- 操作项根据输入合法性设置 `enabled`。

适用场景：

- 需要输入域名、URL、名称等短文本。
- 输入完成后执行添加、测试或保存。

#### 5.1.6 危险操作页

代表：`DataCleanupScreen.kt`

结构：

- 页面顶部先用 `SettingsInfoText` 说明风险。
- 危险操作使用 `SettingsTextItem`。
- `textColor` 设置为 `MaterialTheme.colorScheme.error`。
- 点击后不直接执行，先设置 `pendingAction` 并显示 `AlertDialog`。
- 确认后在 IO 协程中执行清理，并回到主线程更新 UI。

适用场景：

- 删除、清空、重置、不可恢复操作。

规范：

- 危险项必须使用错误色。
- 危险项必须有二次确认。
- 弹窗文案要明确删除范围和后果。

### 5.2 二级页滚动容器选择

当前有两种写法：

- `Column + verticalScroll`：适合固定条目较少的页面，例如缓存设置、日志保留、数据清理。
- `LazyColumn`：适合列表、数据加载后条目较多或可能增长的页面，例如服务商管理、规则管理、竞速服务商。

迁移时建议按数据规模选择，不要所有页面都使用 `LazyColumn`。固定小页面使用 `Column` 更直观。

### 5.3 二级页加载态

通用加载态由 `SettingsLoadingContent` 实现：

```kotlin
Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    CircularProgressIndicator()
}
```

页面使用方式：

```kotlin
SettingsScaffold(title = "标题", onBack = onBack) { innerPadding ->
    if (initialLoading) {
        SettingsLoadingContent(modifier = Modifier.padding(innerPadding))
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // content
        }
    }
}
```

## 6. 统一组件 API 说明

### 6.1 SettingsScaffold

职责：统一设置页顶部栏和返回行为。

参数：

- `title`：页面标题。
- `onBack`：返回回调。
- `actions`：TopAppBar 右侧操作区，默认空。
- `content`：内容区，接收 `PaddingValues`。

设计要点：

- 使用 `TopAppBar`。
- 返回图标使用 `Icons.AutoMirrored.Filled.ArrowBack`，支持 RTL 自动镜像。
- 内容区不强制滚动，由页面自行决定 `Column` 或 `LazyColumn`。

### 6.2 SettingsGroup

职责：设置分组卡片。

特点：

- `fillMaxWidth()`。
- 水平外边距 `16.dp`。
- 圆角 `12.dp`。
- 背景色 `surfaceContainer`。
- 内部是 `Column`，直接放多个设置项和分隔线。

迁移时，这是视觉风格最核心的组件。

### 6.3 SettingsGroupTitle

职责：分组标题。

特点：

- 使用 `titleSmall`。
- 颜色 `onSurfaceVariant`。
- 左右 `32.dp`，使标题与列表项正文对齐。
- 顶部 `24.dp`，分组之间形成足够距离。

### 6.4 SettingsInfoText

职责：说明文字、提示文字、状态摘要。

特点：

- 使用 `bodySmall`。
- 颜色 `onSurfaceVariant`。
- 左右 `32.dp`。
- 通常放在分组下方，也可以作为页面开头提示。

### 6.5 SettingsItem

职责：所有设置行的基础组件。

参数：

- `title`：主标题。
- `subtitle`：副标题，可为空。
- `leadingIcon`：左侧图标，可为空。
- `titleColor`：主标题颜色，可用于危险项或强调项。
- `enabled`：控制点击和禁用样式。
- `onClick`：为空时不可点击，不为空时整行可点击。
- `trailing`：右侧内容插槽。

布局：

- 行最小高度 `44.dp`。
- 水平内边距 `16.dp`，垂直内边距 `12.dp`。
- 左侧标题区 `weight(1f)`，右侧 trailing 按内容宽度。
- 标题和副标题垂直排列。
- `onClick != null` 时对整行加 `clickable`。

这是所有派生组件的基础。

### 6.6 SettingsSwitchItem

职责：开关设置。

行为：

- 整行点击会切换状态。
- 右侧显示 Material 3 `Switch`。
- `Switch` 自身也绑定同一个 `onCheckedChange`。

使用场景：

- 总开关。
- 即时生效的布尔配置。

### 6.7 SettingsCheckboxItem

职责：多选设置。

行为：

- 整行点击切换勾选。
- 右侧显示 Material 3 `Checkbox`。
- 支持 `enabled`。

使用场景：

- 多个服务商选择。
- 多个规则/权限/能力选择。

### 6.8 SettingsNavigationItem

职责：导航到下一级页面。

特点：

- 右侧可显示 `value`。
- 右侧显示自适应方向的箭头 `KeyboardArrowRight`。
- 整行可点击。

使用场景：

- 一级设置页入口。
- 二级页中跳转到三级管理页或列表页。

### 6.9 SettingsTextItem

职责：普通文本操作项。

特点：

- 可设置 `textColor`，例如危险操作使用 `error`。
- 支持 `enabled`。
- 支持自定义 trailing。

使用场景：

- “添加到屏蔽规则”
- “清空 DNS 缓存”
- “删除 DNS 请求日志”

### 6.10 SettingsRadioItem

职责：单选项。

特点：

- 选中时右侧显示 `Icons.Default.Check`。
- 未选中时右侧为空。
- 整行可点击。

当前实现不是 Material `RadioButton`，而是 iOS 设置页常见的右侧对勾风格。迁移时如果想保持当前视觉，不要替换成圆形 RadioButton。

### 6.11 SettingsDivider

职责：组内分隔线。

特点：

- `fillMaxWidth()`。
- 颜色 `outlineVariant`。
- 当前实现左右顶到卡片边缘。

使用规则：

- 只放在同一个 `SettingsGroup` 的条目之间。
- 最后一项后不要放。

## 7. 状态与数据实现方式

### 7.1 本地简单设置

简单设置使用 `AppSettings` 封装 `SharedPreferences`。

页面读取方式：

```kotlin
val context = LocalContext.current
var enabled by remember { mutableStateOf(AppSettings.isCacheEnabled(context)) }
```

页面保存方式：

```kotlin
fun saveEnabled(next: Boolean) {
    enabled = next
    AppSettings.setDnsCachePolicy(context, preset.toPolicy(enabled = next))
}
```

适合放在 `AppSettings` 的配置：

- 布尔值。
- 小型枚举。
- 数字档位。
- 字符串配置。
- 少量 ID 集合。

迁移建议：

- 另一个项目可以继续使用 `SharedPreferences`，也可以替换为 DataStore。
- 如果替换为 DataStore，UI 组件层不需要变，只要页面状态读取和保存逻辑换成 Flow 即可。

### 7.2 ViewModel 驱动页面

复杂页面使用 ViewModel，例如：

- `ProviderManagementViewModel`
- `RaceModeSettingsViewModel`
- `RuleManagementViewModel`

页面通过 `collectAsStateWithLifecycle()` 订阅状态：

```kotlin
val providers by viewModel.providers.collectAsStateWithLifecycle()
val initialLoading by viewModel.initialLoading.collectAsStateWithLifecycle()
```

优点：

- 页面只负责展示和触发事件。
- 列表数据、数据库操作、业务校验放在 ViewModel。
- 可使用 `initialLoading` 控制加载态。

迁移建议：

- 组件层不要依赖业务 ViewModel。
- 页面层可以按目标项目业务重写 ViewModel。
- 保留 `collectAsStateWithLifecycle()`，避免生命周期不活跃时继续收集。

### 7.3 协程和后台操作

页面中直接执行的轻量后台操作使用：

```kotlin
val scope = rememberCoroutineScope()

scope.launch(Dispatchers.IO) {
    // database or IO work
}
```

如果操作完成后需要更新 UI，使用：

```kotlin
withContext(Dispatchers.Main) {
    pendingAction = null
}
```

更复杂的业务操作应下沉到 ViewModel，页面只调用 `viewModel.xxx()`。

## 8. 导航实现

导航集中在 `AppNavHost.kt`。

### 8.1 路由定义

所有路由集中在 `Routes` 对象：

```kotlin
object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val CACHE_SETTINGS = "cache_settings"
    const val LOG_RETENTION_SETTINGS = "log_retention_settings"
}
```

迁移时建议继续集中定义路由，避免字符串散落在各页面。

### 8.2 页面注册

一级设置页注册：

```kotlin
composable(Routes.SETTINGS) {
    SettingsScreen(
        onBack = { navController.popBackStack() },
        onNavigateToCacheSettings = { navController.navigate(Routes.CACHE_SETTINGS) },
        onNavigateToLogRetentionSettings = { navController.navigate(Routes.LOG_RETENTION_SETTINGS) }
    )
}
```

二级页注册：

```kotlin
composable(Routes.CACHE_SETTINGS) {
    CacheSettingsScreen(
        onBack = { navController.popBackStack() }
    )
}
```

设计原则：

- 页面不直接持有 `NavController`。
- 页面只接收 `onBack` 和 `onNavigateToXxx` 回调。
- 导航行为集中在 `AppNavHost`。

这让页面更容易迁移和预览。

### 8.3 转场动画

当前导航使用横向滑动 + 淡入淡出：

- 正向进入：从右侧滑入。
- 正向退出：向左退出三分之一距离。
- 返回进入：从左侧三分之一距离滑入。
- 返回退出：向右滑出。
- 动画时长：`300ms`。

对应实现：

```kotlin
private const val NAV_ANIM_DURATION = 300
```

如果目标项目已有自己的转场规范，可以只迁移页面组件，不迁移转场。

## 9. 迁移步骤

### 9.1 必要依赖

目标项目需要 Compose、Material 3、Material Icons、Navigation Compose 和 Lifecycle Compose。

当前项目依赖包括：

```kotlin
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.compose.material.icons.core)
implementation(libs.androidx.compose.material.icons.extended)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.navigation.compose)
```

如果目标项目不需要 ViewModel 页面，只迁移静态设置页，则至少需要：

- Compose UI
- Material 3
- Material Icons Core 或 Extended
- Activity Compose

### 9.2 复制组件

优先复制：

```text
app/src/main/java/com/haoze/dnssr/ui/components/SettingsComponents.kt
```

复制后需要修改：

- `package com.haoze.dnssr.ui.components` 改成目标项目包名。
- 导入路径随包名自动调整。

组件本身不依赖 DNSSR 业务类，迁移成本最低。

### 9.3 复制主题或接入现有主题

如果目标项目没有 Material 3 主题，可参考：

```text
app/src/main/java/com/haoze/dnssr/ui/theme/Theme.kt
app/src/main/java/com/haoze/dnssr/ui/theme/Color.kt
app/src/main/java/com/haoze/dnssr/ui/theme/Type.kt
```

如果目标项目已有主题，只需要保证设置页面运行在 `MaterialTheme` 下：

```kotlin
YourTheme {
    AppNavHost(...)
}
```

### 9.4 建立一级设置页

复制 `SettingsScreen.kt` 的结构，但替换业务项。

迁移时保留：

- `SettingsScaffold(title = "设置")`
- `Column + verticalScroll`
- `SettingsGroupTitle`
- `SettingsGroup`
- `SettingsNavigationItem`
- `SettingsDivider`

替换：

- 当前状态摘要读取逻辑。
- 分组名称。
- 导航回调。

### 9.5 建立二级设置页

按页面类型选择模板：

- 布尔配置：`SettingsSwitchItem`
- 枚举配置：`SettingsRadioItem`
- 多选配置：`SettingsCheckboxItem`
- 下级入口：`SettingsNavigationItem`
- 普通操作：`SettingsTextItem`
- 危险操作：`SettingsTextItem(textColor = MaterialTheme.colorScheme.error)`
- 数据加载：`SettingsLoadingContent`
- 输入：`OutlinedTextField(shape = SettingsCornerShape)`

### 9.6 接入导航

参考 `AppNavHost.kt`：

- 在 `Routes` 对象添加路由。
- 在 `NavHost` 中添加 `composable`。
- 页面参数传 `onBack = { navController.popBackStack() }`。
- 从一级页传入 `onNavigateToXxx = { navController.navigate(Routes.XXX) }`。

### 9.7 替换设置存储

如果目标项目使用 `SharedPreferences`：

- 可以仿照 `AppSettings.kt` 建一个 `object ProjectSettings`。
- 每个配置提供 `getXxx()` 和 `setXxx()`。
- 页面中用 `remember` 创建本地 UI 状态，保存时同步写入。

如果目标项目使用 DataStore：

- ViewModel 暴露 `StateFlow<UiState>`。
- 页面使用 `collectAsStateWithLifecycle()`。
- 点击事件调用 ViewModel 的保存方法。

## 10. 新增设置项示例

### 10.1 在一级页添加一个入口

```kotlin
SettingsGroupTitle("网络")
SettingsGroup {
    SettingsNavigationItem(
        title = "代理设置",
        subtitle = "配置代理地址、端口和认证信息",
        value = proxySummary,
        onClick = onNavigateToProxySettings
    )
}
```

同时在 `SettingsScreen` 参数中增加：

```kotlin
onNavigateToProxySettings: () -> Unit
```

并在 `AppNavHost` 注册路由。

### 10.2 创建一个单选二级页

```kotlin
private enum class ThemeMode(val displayName: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

@Composable
fun ThemeModeSettingsScreen(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    SettingsScaffold(
        title = "外观模式",
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SettingsGroupTitle("外观")
            SettingsGroup {
                ThemeMode.values().forEachIndexed { index, mode ->
                    SettingsRadioItem(
                        title = mode.displayName,
                        selected = selected == mode,
                        onClick = { onSelect(mode) }
                    )
                    if (index < ThemeMode.values().lastIndex) {
                        SettingsDivider()
                    }
                }
            }
            SettingsInfoText("选择应用界面的明暗显示方式。")
        }
    }
}
```

### 10.3 创建一个危险操作页

```kotlin
var showConfirm by remember { mutableStateOf(false) }

SettingsGroupTitle("重置")
SettingsGroup {
    SettingsTextItem(
        title = "恢复默认设置",
        subtitle = "清除自定义配置并恢复初始值",
        textColor = MaterialTheme.colorScheme.error,
        onClick = { showConfirm = true }
    )
}

if (showConfirm) {
    AlertDialog(
        onDismissRequest = { showConfirm = false },
        title = { Text("恢复默认设置") },
        text = { Text("确定要清除所有自定义配置吗？此操作无法恢复。") },
        confirmButton = {
            TextButton(onClick = {
                resetSettings()
                showConfirm = false
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { showConfirm = false }) {
                Text("取消")
            }
        }
    )
}
```

## 11. 文案规范

### 11.1 标题

- 页面标题：短名词或名词短语，例如“设置”“本地 DNS 缓存”“清理本地数据”。
- 分组标题：分类名，例如“缓存”“操作”“规则列表与订阅”。
- 列表项标题：动作或对象名，例如“清空 DNS 缓存”“DNS 日志保留”。

### 11.2 副标题

副标题用于说明功能影响，不重复标题。

推荐：

```text
缓存已解析的域名，减少重复查询
设置请求日志自动清理时间
选择、添加或编辑 DNS over HTTPS 服务
```

不推荐：

```text
点击这里进入设置
这里可以设置 DNS 日志保留
这是一个很有用的功能
```

### 11.3 说明文字

`SettingsInfoText` 用于解释规则、限制和后果。它应该回答用户可能的疑问：

- 这个设置会影响什么？
- 为什么某个按钮不可用？
- 删除后能不能恢复？
- 这个结果是否实时、是否只是本次测试？

### 11.4 危险操作文案

危险操作必须明确对象和后果：

```text
以下操作会立即删除本机数据，删除后无法恢复。
确定要删除所有 DNS 请求日志和竞速统计吗？
```

## 12. 迁移检查清单

迁移完成后按以下清单检查：

- 所有设置页都包在 `MaterialTheme` 下。
- 所有设置页都使用 `SettingsScaffold`。
- 所有内容都应用了 `innerPadding`。
- 一级页只放导航和状态摘要。
- 二级页按功能拆分成清晰分组。
- 组内条目之间有 `SettingsDivider()`，最后一项后没有分隔线。
- 输入框使用 `SettingsCornerShape`。
- 危险操作使用 `MaterialTheme.colorScheme.error`。
- 危险操作有二次确认。
- 列表型页面使用 `LazyColumn`。
- 固定小页面使用 `Column + verticalScroll`。
- 页面不直接持有 `NavController`，只接收导航回调。
- 业务设置存储和 UI 组件解耦。
- 暗色模式下卡片、文字、分隔线仍清晰。
- 长副标题可以自动换行，不会挤压右侧控件。

## 13. 需要重点保留的代码边界

迁移时建议保持以下边界：

- `SettingsComponents.kt`：纯 UI 组件，不引入业务模型。
- `SettingsScreen.kt`：一级设置目录，读取摘要状态，发出导航事件。
- 各二级页：读取和修改对应功能的状态。
- `AppSettings.kt` 或目标项目设置仓库：负责持久化。
- `AppNavHost.kt`：负责路由和页面之间的跳转。

不要把 SharedPreferences、数据库操作或业务模型塞进 `SettingsComponents.kt`。这样组件才能在另一个项目中直接复用。

## 14. 最小迁移版本

如果只想快速复用视觉风格，最小迁移范围如下：

1. 复制 `SettingsComponents.kt`。
2. 确保目标项目使用 Material 3。
3. 新建一个 `SettingsScreen`，使用 `SettingsScaffold`、`SettingsGroupTitle`、`SettingsGroup` 和 `SettingsNavigationItem`。
4. 为每个二级页按需要使用 `SettingsSwitchItem`、`SettingsRadioItem`、`SettingsCheckboxItem`、`SettingsTextItem`。
5. 在目标项目导航中注册一级页和二级页。

这样即可得到与当前项目一致的设置页结构和视觉风格。业务层、存储层和具体页面数量可以按目标项目逐步替换。
