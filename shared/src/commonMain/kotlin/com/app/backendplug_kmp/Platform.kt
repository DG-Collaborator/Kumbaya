package com.app.backendplug_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun currentDate(): String

expect fun ollamaBaseUrl(): String