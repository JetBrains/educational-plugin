package com.jetbrains.edu.learning

import com.intellij.ide.RecentProjectsManagerBase

// BACKCOMPAT: 2022.2. Inline it
fun recentProjectManagerEx(): RecentProjectsManagerBase = RecentProjectsManagerBase.getInstanceEx()
