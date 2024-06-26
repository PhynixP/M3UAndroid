package com.m3u.core.architecture.logger

import com.m3u.core.wrapper.Message
import javax.inject.Qualifier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface Logger {
    fun log(
        text: String,
        level: Int = Message.LEVEL_INFO,
        tag: String = Thread.currentThread().name,
        duration: Duration = 5.seconds,
        type: Int = Message.TYPE_SNACK
    )

    fun log(
        throwable: Throwable,
        level: Int = Message.LEVEL_ERROR,
        tag: String = Thread.currentThread().name
    )

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class MessageImpl
}
