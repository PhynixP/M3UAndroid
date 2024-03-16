package com.m3u.data.repository

import android.net.Uri
import com.m3u.data.api.xtream.XtreamStreamInfo
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.PlaylistWithCount
import com.m3u.data.database.model.PlaylistWithStreams
import com.m3u.data.database.model.Stream
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observeAll(): Flow<List<Playlist>>
    suspend fun get(url: String): Playlist?
    fun observe(url: String): Flow<Playlist?>
    fun observeWithStreams(url: String): Flow<PlaylistWithStreams?>
    suspend fun getWithStreams(url: String): PlaylistWithStreams?

    suspend fun m3u(
        title: String,
        url: String,
        callback: (count: Int, total: Int) -> Unit = { _, _ -> }
    )

    suspend fun xtream(
        title: String,
        basicUrl: String,
        username: String,
        password: String,
        type: String?
    )

    suspend fun refresh(url: String)

    suspend fun unsubscribe(url: String): Playlist?

    suspend fun rename(url: String, target: String)

    suspend fun backupOrThrow(uri: Uri)

    suspend fun restoreOrThrow(uri: Uri)

    suspend fun pinOrUnpinCategory(url: String, category: String)

    suspend fun hideOrUnhideCategory(url: String, category: String)

    suspend fun updateUserAgent(url: String, userAgent: String?)

    fun observePlaylistCounts(): Flow<List<PlaylistWithCount>>

    suspend fun readEpisodesOrThrow(series: Stream): List<XtreamStreamInfo.Episode>
}
