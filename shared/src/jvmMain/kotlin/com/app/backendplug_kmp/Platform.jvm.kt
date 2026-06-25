package com.app.backendplug_kmp

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun ollamaBaseUrl(): String = "http://localhost:11434"

actual fun currentDate(): String =
    java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy, h:mm a"))