package com.jetbrains.edu.android

import com.android.tools.idea.gradle.plugin.LatestKnownPluginVersionProvider

fun getLatestAndroidGradlePluginVersion(): String = LatestKnownPluginVersionProvider.INSTANCE.get()
