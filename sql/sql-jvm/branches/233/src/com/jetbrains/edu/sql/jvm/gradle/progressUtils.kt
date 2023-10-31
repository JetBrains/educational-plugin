package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.withRawProgressReporter
import kotlinx.coroutines.CoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts

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

