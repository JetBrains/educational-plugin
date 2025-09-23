package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl

fun RefreshQueueImpl.Companion.isRefreshInProgress(): Boolean = isRefreshInProgress