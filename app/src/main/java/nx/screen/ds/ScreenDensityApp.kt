package nx.screen.ds

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nx.screen.ds.data.AppSettings
import nx.screen.ds.data.UiSystem
import nx.screen.ds.data.WallpaperData
import nx.screen.ds.ui.AboutScreen
import nx.screen.ds.ui.AppsScreen
import nx.screen.ds.ui.GeneralScreen
import nx.screen.ds.ui.HomeScreen
import nx.screen.ds.ui.LanguagesScreen
import nx.screen.ds.ui.LocalScaledInsets
import nx.screen.ds.ui.SettingsScreen
import nx.screen.ds.ui.SettingsViewModel
import nx.screen.ds.ui.ThemeScreen
import nx.screen.ds.ui.theme.AppBackdrop
import nx.screen.ds.ui.theme.AppTextButton
import nx.screen.ds.ui.theme.AppsIcon
import nx.screen.ds.ui.theme.GearIcon
import nx.screen.ds.ui.theme.GlassEffect
import nx.screen.ds.ui.theme.HomeIcon
import nx.screen.ds.ui.theme.InfoCircleIcon
import nx.screen.ds.ui.theme.LocalGlassEffect
import nx.screen.ds.ui.theme.LocalUiSystem
import nx.screen.ds.ui.theme.MusicPlayer
import kotlin.math.roundToInt

private object Routes {
    const val THEME = "theme"
    const val GENERAL = "general"
    const val LANGUAGES = "languages"
    const val SUBROOT = "subroot"
}

@Composable
fun ScreenDensityApp(
    settings: AppSettings = AppSettings(),
    wallpaper: WallpaperData? = null,
    activity: Activity? = null,
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val navController = rememberNavController()
    navController.enableOnBackPressed(false)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val scheme = MaterialTheme.colorScheme
    val isMainTab = route == null || route == Routes.SUBROOT
    var savedPage by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = savedPage, pageCount = { TAB_ITEMS.size })
    LaunchedEffect(pagerState.currentPage) { savedPage = pagerState.currentPage }
    var pagerVisible by remember { mutableStateOf(true) }
    LaunchedEffect(isMainTab) { pagerVisible = isMainTab }
    val scope = rememberCoroutineScope()
    var showExitDialog by remember { mutableStateOf(false) }
    var lastBackPressed by remember { mutableStateOf(0L) }
    BackHandler {
        if (isMainTab) {
            if (pagerState.currentPage > 0) {
                lastBackPressed = 0L
                scope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) }
            } else {
                val now = System.currentTimeMillis()
                if (now - lastBackPressed < 2000) {
                    showExitDialog = true
                } else {
                    lastBackPressed = now
                }
            }
        } else {
            navController.popBackStack()
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.exit_title)) },
            text = { Text(stringResource(R.string.exit_text)) },
            confirmButton = {
                AppTextButton(onClick = { activity?.finish() }) {
                    Text(stringResource(R.string.exit_confirm))
                }
            },
            dismissButton = {
                AppTextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    val glassAlpha = remember(settings.glass, settings.glassTransparency) {
        0.45f + 0.35f * (1f - settings.glassTransparency.coerceIn(0f, 1f))
    }
    CompositionLocalProvider(
        LocalGlassEffect provides GlassEffect(active = settings.glass, surfaceAlpha = glassAlpha),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val music by settingsViewModel.music.collectAsState()
            music?.let { musicData ->
                MusicPlayer(
                    path = musicData.filePath,
                    volume = settings.musicVolume,
                    initialPosition = settings.musicPosition,
                    onPosition = settingsViewModel::saveMusicPosition,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AppBackdrop(
                glass = settings.glass,
                wallpaper = wallpaper,
                blur = settings.glassBlur,
                transparency = settings.glassTransparency,
                glow = settings.glassGlow,
                static = settings.batterySaver,
                volume = settings.videoVolume,
            )
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = LocalScaledInsets.current?.safe ?: WindowInsets.safeDrawing,
            bottomBar = {
                if (isMainTab) {
                    MainNavigationBar(
                        pagerState = pagerState,
                        scheme = scheme,
                        uiSystem = settings.uiSystem,
                        floating = settings.floatingNavBar,
                        onSelectPage = { page ->
                            if (pagerState.currentPage != page) {
                                scope.launch { pagerState.scrollToPage(page) }
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (pagerVisible) 1f else 0f),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 0,
                    ) { page ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (page) {
                                0 -> HomeScreen()
                                1 -> AppsScreen()
                                2 -> SettingsScreen(
                                    onOpenTheme = {
                                        navController.navigate(Routes.THEME) { launchSingleTop = true }
                                    },
                                    onOpenLanguages = {
                                        navController.navigate(Routes.LANGUAGES) { launchSingleTop = true }
                                    },
                                    onOpenGeneral = {
                                        navController.navigate(Routes.GENERAL) { launchSingleTop = true }
                                    },
                                    viewModel = settingsViewModel,
                                )
                                else -> AboutScreen(appTitle = settings.appTitle)
                            }
                        }
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = Routes.SUBROOT,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None },
                ) {
                    composable(Routes.SUBROOT) { }
                    composable(Routes.THEME) {
                        ThemeScreen(onBack = { navController.popBackStack() }, viewModel = settingsViewModel)
                    }
                    composable(Routes.GENERAL) {
                        GeneralScreen(onBack = { navController.popBackStack() }, viewModel = settingsViewModel)
                    }
                    composable(Routes.LANGUAGES) {
                        LanguagesScreen(onBack = { navController.popBackStack() }, viewModel = settingsViewModel)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MainNavigationBar(
    pagerState: PagerState,
    scheme: androidx.compose.material3.ColorScheme,
    uiSystem: UiSystem,
    floating: Boolean,
    onSelectPage: (Int) -> Unit,
) {
    val isFloating = floating
    val navInsets = LocalScaledInsets.current?.navigationBars ?: WindowInsets.navigationBars
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(navInsets),
    ) {
        if (isFloating) {
            FloatingNavBar(
                pagerState = pagerState,
                scheme = scheme,
                onSelectPage = onSelectPage,
            )
        } else {
            StandardNavBar(
                pagerState = pagerState,
                scheme = scheme,
                uiSystem = uiSystem,
                onSelectPage = onSelectPage,
            )
        }
    }
}

@Composable
private fun StandardNavBar(
    pagerState: PagerState,
    scheme: androidx.compose.material3.ColorScheme,
    uiSystem: UiSystem,
    onSelectPage: (Int) -> Unit,
) {
    val glass = LocalGlassEffect.current
    NavigationBar(
        containerColor = scheme.surfaceContainer.copy(
            alpha = if (glass.active) glass.surfaceAlpha else 0.92f,
        ),
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        TAB_ITEMS.forEachIndexed { index, tab ->
            val selected = pagerState.currentPage == index
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectPage(index) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = {
                    Text(
                        stringResource(tab.labelRes),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                colors = navItemColors(scheme, uiSystem),
            )
        }
    }
}

private data class TabItem(val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TAB_ITEMS = listOf(
    TabItem(R.string.tab_system, HomeIcon),
    TabItem(R.string.tab_apps, AppsIcon),
    TabItem(R.string.tab_settings, GearIcon),
    TabItem(R.string.tab_about, InfoCircleIcon),
)

@Composable
private fun FloatingNavBar(
    pagerState: PagerState,
    scheme: androidx.compose.material3.ColorScheme,
    onSelectPage: (Int) -> Unit,
) {
    val itemSize = 56.dp
    val itemSpacing = 4.dp
    val containerPadding = 7.dp
    val itemShape = if (LocalUiSystem.current == UiSystem.MIUIX) {
        MaterialTheme.shapes.large
    } else {
        RoundedCornerShape(28.dp)
    }
    val density = LocalDensity.current
    val navInsets = LocalScaledInsets.current?.navigationBars ?: WindowInsets.navigationBars
    val bottomPad = with(density) {
        navInsets.asPaddingValues().calculateBottomPadding()
    }

    val animatedIndex = remember { Animatable(pagerState.currentPage.toFloat()) }
    LaunchedEffect(pagerState.currentPage) {
        animatedIndex.animateTo(
            targetValue = pagerState.currentPage.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = (bottomPad * 0.5f).coerceAtMost(20.dp)),
    ) {
        val horizontalPadding = when {
            maxWidth > 600.dp -> 32.dp
            maxWidth > 400.dp -> 24.dp
            else -> 16.dp
        }
        val innerWidth = maxWidth - horizontalPadding * 2
        val itemWidth = (innerWidth - containerPadding * 2 - itemSpacing * (TAB_ITEMS.size - 1)) / TAB_ITEMS.size
        val indicatorOffsetX = with(density) {
            (containerPadding + animatedIndex.value * (itemWidth + itemSpacing)).toPx().roundToInt()
        }
        val indicatorOffsetY = with(density) { containerPadding.toPx().roundToInt() }
        val glass = LocalGlassEffect.current
        val barAlpha = if (glass.active) glass.surfaceAlpha else 0.92f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.wrapContentWidth(),
                shape = RoundedCornerShape(34.dp),
                color = scheme.surfaceContainer.copy(alpha = barAlpha),
                tonalElevation = 3.dp,
                shadowElevation = 12.dp,
            ) {
                Box(
                    modifier = Modifier.size(
                        width = itemWidth * TAB_ITEMS.size + itemSpacing * (TAB_ITEMS.size - 1) + containerPadding * 2,
                        height = itemSize + containerPadding * 2,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(indicatorOffsetX, indicatorOffsetY) }
                            .size(itemWidth, itemSize)
                            .clip(itemShape)
                            .background(scheme.secondaryContainer),
                    )
                    TAB_ITEMS.forEachIndexed { index, tab ->
                        val selected = pagerState.currentPage == index
                        val x = with(density) {
                            (containerPadding + index * (itemWidth + itemSpacing)).toPx().roundToInt()
                        }
                        FloatingNavItem(
                            index = index,
                            tab = tab,
                            selected = selected,
                            onClick = { onSelectPage(index) },
                            scheme = scheme,
                            offsetX = x,
                            offsetY = indicatorOffsetY,
                            width = itemWidth,
                            height = itemSize,
                            shape = itemShape,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    index: Int,
    tab: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme,
    offsetX: Int = 0,
    offsetY: Int = 0,
    width: androidx.compose.ui.unit.Dp = 56.dp,
    height: androidx.compose.ui.unit.Dp = 56.dp,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(width, height)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                tab.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
            )
            Text(
                stringResource(tab.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun navItemColors(scheme: androidx.compose.material3.ColorScheme, uiSystem: UiSystem) =
    NavigationBarItemDefaults.colors(
        selectedIconColor = if (uiSystem == UiSystem.MIUIX) scheme.primary else scheme.onSecondaryContainer,
        selectedTextColor = if (uiSystem == UiSystem.MIUIX) scheme.primary else scheme.onSecondaryContainer,
        indicatorColor = scheme.secondaryContainer,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
    )
