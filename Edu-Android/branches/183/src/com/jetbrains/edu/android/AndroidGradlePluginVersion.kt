package com.jetbrains.edu.android

import com.android.tools.idea.gradle.plugin.AndroidPluginGeneration

fun getLatestAndroidGradlePluginVersion(): String = AndroidPluginGeneration.ORIGINAL.latestKnownVersion
