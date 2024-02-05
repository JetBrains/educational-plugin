package com.jetbrains.edu.learning.command

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// BACKCOMPAT: 2023.2. Inline it
typealias Application = com.intellij.openapi.application.Application

// BACKCOMPAT: 2023.2. Inline it
suspend fun <T> span(
  name: String,
  context: CoroutineContext = EmptyCoroutineContext,
  action: suspend CoroutineScope.() -> T
): T = com.intellij.platform.diagnostic.telemetry.impl.span(name, context, action)
