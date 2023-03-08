package com.m3u.features.crash.screen.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m3u.features.crash.components.FileItem

@Composable
internal fun DetailScreen(
    path: String,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel()
) {
    LaunchedEffect(path) {
        viewModel.onEvent(DetailEvent.Init(path))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val file = state.file
    file?.let {
        FileItem(
            file = it,
            asScreen = true,
            modifier = modifier
        )
    }
}