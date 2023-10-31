package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.progress.withRawProgressReporter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import kotlinx.coroutines.CoroutineScope

// BACKCOMPAT: 2023.2. Inline it
suspend fun <X> withRawProgressReporter(action: suspend CoroutineScope.() -> X) = withRawProgressReporter(action)

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
suspend fun <T> withBackgroundProgress(
  project: Project,
  title: @NlsContexts.ProgressTitle String,
  cancellable: Boolean,
  action: suspend CoroutineScope.() -> T
): T = withBackgroundProgress(project, title, cancellable, action)
