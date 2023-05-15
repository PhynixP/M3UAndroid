package com.m3u.features.main.components

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.m3u.features.main.R
import com.m3u.ui.components.ProgressLottie

@Composable
internal fun Loading(
    @FloatRange(0.0, 1.0) offset: Float,
    modifier: Modifier = Modifier
) {
    ProgressLottie(
        raw = R.raw.loading,
        progress = { offset },
        modifier = modifier
    )
}