package com.m3u.features.playlist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewModelScope
import com.m3u.core.Contracts
import com.m3u.core.architecture.logger.Logger
import com.m3u.core.architecture.pref.Pref
import com.m3u.core.architecture.pref.observeAsFlow
import com.m3u.core.architecture.viewmodel.BaseViewModel
import com.m3u.core.wrapper.chain
import com.m3u.core.wrapper.chainmap
import com.m3u.core.wrapper.eventOf
import com.m3u.core.wrapper.failure
import com.m3u.core.wrapper.success
import com.m3u.data.database.entity.Stream
import com.m3u.data.repository.MediaRepository
import com.m3u.data.repository.PlaylistRepository
import com.m3u.data.repository.StreamRepository
import com.m3u.data.repository.refresh
import com.m3u.data.service.PlayerManager
import com.m3u.features.playlist.PlaylistMessage.StreamCoverSaved
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val streamRepository: StreamRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository,
    playerManager: PlayerManager,
    private val pref: Pref,
    @Logger.Ui private val logger: Logger
) : BaseViewModel<PlaylistState, PlaylistEvent, PlaylistMessage>(
    emptyState = PlaylistState()
) {
    override fun onEvent(event: PlaylistEvent) {
        when (event) {
            is PlaylistEvent.Observe -> observe(event.playlistUrl)
            PlaylistEvent.Refresh -> refresh()
            is PlaylistEvent.Favourite -> favourite(event)
            PlaylistEvent.ScrollUp -> scrollUp()
            is PlaylistEvent.Ban -> ban(event)
            is PlaylistEvent.SavePicture -> savePicture(event)
            is PlaylistEvent.Query -> query(event)
            is PlaylistEvent.CreateShortcut -> createShortcut(event.context, event.id)
        }
    }

    private val zappingMode = pref
        .observeAsFlow { it.zappingMode }
        .stateIn(
            scope = viewModelScope,
            initialValue = Pref.DEFAULT_ZAPPING_MODE,
            started = SharingStarted.WhileSubscribed(5_000)
        )

    val floating = combine(
        zappingMode,
        playerManager.url,
        streamRepository.observeAll()
    ) { zappingMode, url, streams ->
        if (!zappingMode) null
        else streams.find { it.url == url }
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(5_000)
        )

    private var observeJob: Job? = null
    private fun observe(playlistUrl: String) {
        if (playlistUrl.isEmpty()) {
            onMessage(PlaylistMessage.PlaylistUrlNotFound)
            return
        }
        observeJob?.cancel()
        observeJob = playlistRepository
            .observeWithStreams(playlistUrl)
            .combine(_query) { current, query ->
                val playlist = current?.playlist
                val streams = current?.streams ?: emptyList()
                val channels = streams.toChannels(query)
                playlist to channels
            }
            .onEach { result ->
                val playlist = result.first
                val channels = result.second
                writable.update { prev ->
                    prev.copy(
                        url = playlist?.url.orEmpty(),
                        channels = channels
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        val url = readable.url
        playlistRepository
            .refresh(url, pref.playlistStrategy)
            .chain()
            .onEach {
                writable.update { prev ->
                    prev.copy(
                        fetching = !it.isCompleted
                    )
                }
            }
            .failure(logger::log)
            .launchIn(viewModelScope)
    }

    private fun favourite(event: PlaylistEvent.Favourite) {
        viewModelScope.launch {
            val id = event.id
            val target = event.target
            streamRepository.setFavourite(id, target)
        }
    }

    private fun scrollUp() {
        writable.update {
            it.copy(
                scrollUp = eventOf(Unit)
            )
        }
    }

    private fun savePicture(event: PlaylistEvent.SavePicture) {
        val id = event.id
        viewModelScope.launch {
            val stream = streamRepository.get(id)
            if (stream == null) {
                onMessage(PlaylistMessage.StreamNotFound)
                return@launch
            }
            val url = stream.cover
            if (url.isNullOrEmpty()) {
                onMessage(PlaylistMessage.StreamCoverNotFound)
                return@launch
            }
            mediaRepository
                .savePicture(url)
                .chain()
                .chainmap { StreamCoverSaved(it.absolutePath) }
                .success { onMessage(it) }
                .failure(logger::log)
                .launchIn(this)
        }
    }

    private fun ban(event: PlaylistEvent.Ban) {
        viewModelScope.launch {
            val id = event.id
            val target = event.target
            val stream = streamRepository.get(id)
            if (stream == null) {
                onMessage(PlaylistMessage.StreamNotFound)
            } else {
                streamRepository.ban(stream.id, target)
            }
        }
    }

    private fun createShortcut(context: Context, id: Int) {
        val shortcutId = "stream_$id"
        viewModelScope.launch {
            val stream = streamRepository.get(id) ?: return@launch
            val shortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(stream.title)
                .setLongLabel(stream.url)
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_media_play))
                .setIntent(
                    Intent(Intent.ACTION_VIEW).apply {
                        component = ComponentName.createRelative(
                            context,
                            Contracts.PLAYER_ACTIVITY
                        )
                        putExtra(Contracts.PLAYER_SHORTCUT_STREAM_URL, stream.url)
                    }
                )
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfo)
        }
    }

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private fun query(event: PlaylistEvent.Query) {
        val text = event.text
        _query.update { text }
    }

    private fun List<Stream>.toChannels(query: CharSequence): List<Channel> =
        filter { it.title.contains(query, true) }
            .groupBy { it.group }
            .toList()
            .map { Channel(it.first, it.second) }
}