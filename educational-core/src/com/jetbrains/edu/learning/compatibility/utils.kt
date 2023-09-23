package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils

// BACKCOMPAT: 2023.2. Use `PlatformUtils.isRustRover()` instead
@Suppress("UnstableApiUsage")
fun isRustRover(): Boolean = PlatformUtils.getPlatformPrefix() == "RustRover"