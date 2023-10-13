package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.progress.withRawProgressReporter
import kotlinx.coroutines.CoroutineScope

suspend fun <X> withRawProgressReporter(action: suspend CoroutineScope.() -> X) = withRawProgressReporter(action)