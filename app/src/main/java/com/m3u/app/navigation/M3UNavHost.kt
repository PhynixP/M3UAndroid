package com.m3u.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.m3u.features.favorite.navigation.favouriteScreen
import com.m3u.features.feed.navigation.feedScreen
import com.m3u.features.live.navigation.liveScreen
import com.m3u.features.main.navgation.mainNavigationRoute
import com.m3u.features.main.navgation.mainScreen
import com.m3u.features.setting.navigation.settingScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun M3UNavHost(
    navController: NavHostController,
    navigateToDestination: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = mainNavigationRoute
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(0)) },
        exitTransition = { fadeOut(tween(0)) },
        popEnterTransition = { fadeIn(tween(0)) },
        popExitTransition = { fadeOut(tween(0)) },
    ) {
        mainScreen(
            navigateToFeed = { url ->
                navigateToDestination(Destination.Feed(url))
            }
        )
        favouriteScreen(
            navigateToLive = { id ->
                navigateToDestination(Destination.Live(id))
            }
        )
        settingScreen()
        liveScreen()
        feedScreen(
            navigateToLive = { id ->
                navigateToDestination(Destination.Live(id))
            }
        )
    }
}