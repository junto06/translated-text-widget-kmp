package com.sdk.translation

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createSettings(context: Any?): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
