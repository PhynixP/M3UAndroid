package com.m3u.features.setting

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChangeCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m3u.core.architecture.preferences.hiltPreferences
import com.m3u.core.unit.DataUnit
import com.m3u.core.util.basic.title
import com.m3u.data.database.model.ColorScheme
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.Stream
import com.m3u.features.setting.components.CanvasBottomSheet
import com.m3u.features.setting.fragments.AppearanceFragment
import com.m3u.features.setting.fragments.SubscriptionsFragment
import com.m3u.features.setting.fragments.preferences.PreferencesFragment
import com.m3u.i18n.R.string
import com.m3u.material.ktx.isTelevision
import com.m3u.material.model.LocalHazeState
import com.m3u.ui.Destination
import com.m3u.ui.EventHandler
import com.m3u.ui.Events
import com.m3u.ui.LocalRootDestination
import com.m3u.ui.SettingDestination
import com.m3u.ui.helper.Fob
import com.m3u.ui.helper.Metadata
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.haze

@Composable
fun SettingRoute(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = hiltViewModel()
) {
    val tv = isTelevision()
    val controller = LocalSoftwareKeyboardController.current

    val colorSchemes by viewModel.colorSchemes.collectAsStateWithLifecycle()
    val epgs by viewModel.epgs.collectAsStateWithLifecycle()
    val hiddenStreams by viewModel.hiddenStreams.collectAsStateWithLifecycle()
    val hiddenCategoriesWithPlaylists by viewModel.hiddenCategoriesWithPlaylists.collectAsStateWithLifecycle()
    val backingUpOrRestoring by viewModel.backingUpOrRestoring.collectAsStateWithLifecycle()

    val cacheSpace by viewModel.cacheSpace.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    var colorInt: Int? by remember { mutableStateOf(null) }
    var isDark: Boolean? by remember { mutableStateOf(null) }

    val createDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            viewModel.backup(uri)
        }
    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            viewModel.restore(uri)
        }

    val backup = {
        val filename = "Backup_${System.currentTimeMillis()}.txt"
        createDocumentLauncher.launch(filename)
    }
    val restore = {
        openDocumentLauncher.launch(arrayOf("text/*"))
    }

    SettingScreen(
        versionName = viewModel.versionName,
        versionCode = viewModel.versionCode,
        titleState = viewModel.titleState,
        urlState = viewModel.urlState,
        uriState = viewModel.uriState,
        basicUrlState = viewModel.basicUrlState,
        usernameState = viewModel.usernameState,
        passwordState = viewModel.passwordState,
        epgState = viewModel.epgState,
        localStorageState = viewModel.localStorageState,
        selectedState = viewModel.selectedState,
        forTvState = viewModel.forTvState,
        backingUpOrRestoring = backingUpOrRestoring,
        epgs = epgs,
        hiddenStreams = hiddenStreams,
        hiddenCategoriesWithPlaylists = hiddenCategoriesWithPlaylists,
        cacheSpace = cacheSpace,
        backup = backup,
        restore = restore,
        colorSchemes = colorSchemes,
        openColorCanvas = { c, i ->
            colorInt = c
            isDark = i
        },
        restoreSchemes = viewModel::restoreSchemes,
        onClipboard = { viewModel.onClipboard(it) },
        onClearCache = { viewModel.clearCache() },
        onSubscribe = {
            controller?.hide()
            viewModel.subscribe()
        },
        onUnhideStream = { viewModel.onUnhideStream(it) },
        onUnhidePlaylistCategory = { playlistUrl, group ->
            viewModel.onUnhidePlaylistCategory(playlistUrl, group)
        },
        onDeleteEpgPlaylist = { viewModel.deleteEpgPlaylist(it) },
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    )
    if (!tv) {
        CanvasBottomSheet(
            sheetState = sheetState,
            colorInt = colorInt,
            isDark = isDark,
            onDismissRequest = {
                colorInt = null
                isDark = null
            }
        )
    }
}

@Composable
private fun SettingScreen(
    titleState: MutableState<String>,
    urlState: MutableState<String>,
    uriState: MutableState<Uri>,
    selectedState: MutableState<DataSource>,
    basicUrlState: MutableState<String>,
    usernameState: MutableState<String>,
    passwordState: MutableState<String>,
    epgState: MutableState<String>,
    localStorageState: MutableState<Boolean>,
    forTvState: MutableState<Boolean>,
    versionName: String,
    versionCode: Int,
    backingUpOrRestoring: BackingUpAndRestoringState,
    onSubscribe: () -> Unit,
    hiddenStreams: List<Stream>,
    hiddenCategoriesWithPlaylists: List<Pair<Playlist, String>>,
    onUnhideStream: (Int) -> Unit,
    onUnhidePlaylistCategory: (playlistUrl: String, group: String) -> Unit,
    backup: () -> Unit,
    restore: () -> Unit,
    onClipboard: (String) -> Unit,
    colorSchemes: List<ColorScheme>,
    openColorCanvas: (Int, Boolean) -> Unit,
    restoreSchemes: () -> Unit,
    cacheSpace: DataUnit,
    onClearCache: () -> Unit,
    epgs: List<Playlist>,
    onDeleteEpgPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val preferences = hiltPreferences()
    val root = LocalRootDestination.current

    val defaultTitle = stringResource(string.ui_title_setting)
    val playlistTitle = stringResource(string.feat_setting_playlist_management)
    val appearanceTitle = stringResource(string.feat_setting_appearance)

    val colorArgb = preferences.argb

    val isPageInfoVisible = root == Destination.Root.Setting

    val navigator = rememberListDetailPaneScaffoldNavigator<SettingDestination>()
    val destination = navigator.currentDestination?.content ?: SettingDestination.Default

    EventHandler(Events.settingDestination) {
        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it)
    }

    if (isPageInfoVisible) {
        LifecycleResumeEffect(destination, defaultTitle, playlistTitle, appearanceTitle) {
            Metadata.title = when (destination) {
                SettingDestination.Default -> defaultTitle
                SettingDestination.Playlists -> playlistTitle
                SettingDestination.Appearance -> appearanceTitle
            }
                .title()
                .let(::AnnotatedString)
            Metadata.color = Color.Unspecified
            Metadata.contentColor = Color.Unspecified
            if (destination != SettingDestination.Default) {
                Metadata.fob = Fob(
                    rootDestination = Destination.Root.Setting,
                    icon = Icons.Rounded.ChangeCircle,
                    iconTextId = string.feat_setting_back_home
                ) {
                    navigator.navigateBack()
                }
            }
            Metadata.actions = emptyList()
            onPauseOrDispose {
                Metadata.fob = null
            }
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            PreferencesFragment(
                fragment = destination,
                contentPadding = contentPadding,
                versionName = versionName,
                versionCode = versionCode,
                navigateToPlaylistManagement = {
                    navigator.navigateTo(
                        pane = ListDetailPaneScaffoldRole.Detail,
                        content = SettingDestination.Playlists
                    )
                },
                navigateToThemeSelector = {
                    navigator.navigateTo(
                        pane = ListDetailPaneScaffoldRole.Detail,
                        content = SettingDestination.Appearance
                    )
                },
                cacheSpace = cacheSpace,
                onClearCache = onClearCache,
                modifier = Modifier.fillMaxSize()
            )
        },
        detailPane = {
            when (destination) {
                SettingDestination.Playlists -> {
                    SubscriptionsFragment(
                        titleState = titleState,
                        urlState = urlState,
                        uriState = uriState,
                        selectedState = selectedState,
                        basicUrlState = basicUrlState,
                        usernameState = usernameState,
                        passwordState = passwordState,
                        epgState = epgState,
                        localStorageState = localStorageState,
                        forTvState = forTvState,
                        backingUpOrRestoring = backingUpOrRestoring,
                        hiddenStreams = hiddenStreams,
                        hiddenCategoriesWithPlaylists = hiddenCategoriesWithPlaylists,
                        onUnhideStream = onUnhideStream,
                        onUnhidePlaylistCategory = onUnhidePlaylistCategory,
                        onClipboard = onClipboard,
                        onSubscribe = onSubscribe,
                        backup = backup,
                        restore = restore,
                        epgs = epgs,
                        onDeleteEpgPlaylist = onDeleteEpgPlaylist,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SettingDestination.Appearance -> {
                    AppearanceFragment(
                        colorSchemes = colorSchemes,
                        colorArgb = colorArgb,
                        openColorCanvas = openColorCanvas,
                        restoreSchemes = restoreSchemes,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {}
            }
        },
        modifier = modifier
            .fillMaxSize()
            .haze(
                LocalHazeState.current,
                HazeDefaults.style(MaterialTheme.colorScheme.surface)
            )
            .testTag("feature:setting")
    )
    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }
}
