package com.m3u.data.parser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface XtreamParser : Parser<XtreamInput, XtreamOutput>

data class XtreamInput(
    val address: String,
    val username: String,
    val password: String
)

data class XtreamOutput(
    val all: List<XtreamData> = emptyList(),
    val allowedOutputFormats: List<String> = emptyList()
)

@Serializable
data class XtreamData(
    @SerialName("added")
    val added: String?,
    @SerialName("category_id")
    val categoryId: String?,
    @SerialName("custom_sid")
    val customSid: String?,
    @SerialName("direct_source")
    val directSource: String?,
    @SerialName("epg_channel_id")
    val epgChannelId: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("num")
    val num: Int?,
    @SerialName("stream_icon")
    val streamIcon: String?,
    @SerialName("stream_id")
    val streamId: Int?,
    @SerialName("stream_type")
    val streamType: String?,
    @SerialName("tv_archive")
    val tvArchive: Int?,
    @SerialName("tv_archive_duration")
    val tvArchiveDuration: Int?
)

