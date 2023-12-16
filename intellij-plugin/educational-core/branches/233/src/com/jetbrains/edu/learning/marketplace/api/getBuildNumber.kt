package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.application.impl.ApplicationInfoImpl

// BACKCOMPAT: 2023.2. Inline it.
@Suppress("UnstableApiUsage")
fun getBuildNumber(): String = ApplicationInfoImpl.getShadowInstanceImpl().pluginCompatibleBuild