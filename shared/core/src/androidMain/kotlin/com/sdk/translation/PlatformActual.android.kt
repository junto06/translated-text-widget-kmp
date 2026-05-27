package com.sdk.translation

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun createSettings(context: Any?): Settings {
    val ctx = context as Context
    return SharedPreferencesSettings(
        ctx.getSharedPreferences("translation_sdk", Context.MODE_PRIVATE)
    )
}
