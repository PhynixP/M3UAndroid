package com.m3u.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.m3u.material.components.Background
import com.m3u.material.model.AppTheme

@Composable
fun M3ULocalProvider(
    helper: Helper = EmptyHelper,
    content: @Composable () -> Unit
) {
    val prevTypography = MaterialTheme.typography
    val typography = remember(prevTypography) {
        prevTypography.withFontFamily(FontFamilies.Titillium)
    }
    AppTheme(
        typography = typography
    ) {
        CompositionLocalProvider(
            LocalHelper provides helper
        ) {
            Background {
                content()
            }
        }
    }
}
