package com.app.backendplug_kmp

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.UIKit.UIDevice
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun ollamaBaseUrl(): String = "http://localhost:11434"

actual fun currentDate(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "EEEE, MMMM d yyyy, h:mm a"
    return formatter.stringFromDate(NSDate())
}

@Composable
actual fun HorizontalScrollBar(state: ScrollState, modifier: Modifier) {}