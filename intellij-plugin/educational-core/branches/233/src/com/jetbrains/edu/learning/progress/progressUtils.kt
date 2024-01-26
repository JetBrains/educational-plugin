@file:Suppress("UnstableApiUsage")

package com.jetbrains.edu.learning.progress

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.platform.util.progress.indeterminateStep
import com.intellij.platform.util.progress.progressStep
import com.intellij.platform.util.progress.withRawProgressReporter
import kotlinx.coroutines.CoroutineScope

typealias ModalTaskOwner = com.intellij.platform.ide.progress.ModalTaskOwner
typealias TaskCancellation = com.intellij.platform.ide.progress.TaskCancellation

// BACKCOMPAT: 2023.2. Inline it
suspend fun <T> withRawProgressReporter(action: suspend CoroutineScope.() -> T): T = withRawProgressReporter(action)

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
suspend fun <T> withBackgroundProgress(
  project: Project,
  title: @NlsContexts.ProgressTitle String,
  cancellable: Boolean,
  action: suspend CoroutineScope.() -> T
): T = withBackgroundProgress(project, title, cancellable, action)

// BACKCOMPAT: 2023.2. Inline it
fun <T> runWithModalProgressBlocking(
  owner: ModalTaskOwner,
  title: @NlsContexts.ProgressTitle String,
  cancellation: TaskCancellation = TaskCancellation.cancellable(),
  action: suspend CoroutineScope.() -> T
): T = runWithModalProgressBlocking(owner, title, cancellation, action)

// BACKCOMPAT: 2023.2. Inline it
suspend fun <T> withModalProgress(
  owner: ModalTaskOwner,
  title: @NlsContexts.ProgressTitle String,
  cancellation: TaskCancellation,
  action: suspend CoroutineScope.() -> T,
): T = withModalProgress(owner, title, cancellation, action)

// BACKCOMPAT: 2023.2. Inline it
suspend fun <T> progressStep(
  endFraction: Double,
  text: @NlsContexts.ProgressText String? = null,
  action: suspend CoroutineScope.() -> T,
): T = progressStep(endFraction, text, action)

// BACKCOMPAT: 2023.2. Inline it
suspend fun <T> indeterminateStep(
  text: @NlsContexts.ProgressText String? = null,
  action: suspend CoroutineScope.() -> T,
): T = indeterminateStep(text, action)
