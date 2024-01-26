@file:Suppress("UnstableApiUsage")

package com.jetbrains.edu.learning.progress

import com.intellij.openapi.progress.indeterminateStep
import com.intellij.openapi.progress.progressStep
import com.intellij.openapi.progress.runBlockingModal
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.progress.withModalProgress
import com.intellij.openapi.progress.withRawProgressReporter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import kotlinx.coroutines.CoroutineScope

typealias ModalTaskOwner = com.intellij.openapi.progress.ModalTaskOwner
typealias TaskCancellation = com.intellij.openapi.progress.TaskCancellation

// BACKCOMPAT: 2023.2. Inline it
suspend fun <T> withRawProgressReporter(action: suspend CoroutineScope.() -> T): T = withRawProgressReporter(action)

// BACKCOMPAT: 2023.2. Inline it
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
): T = runBlockingModal(owner, title, cancellation, action)

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

suspend fun <T> indeterminateStep(
  text: @NlsContexts.ProgressText String? = null,
  action: suspend CoroutineScope.() -> T,
): T = indeterminateStep(text, action)
