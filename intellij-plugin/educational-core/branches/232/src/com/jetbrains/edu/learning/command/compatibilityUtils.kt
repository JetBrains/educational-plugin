package com.jetbrains.edu.learning.command

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias Application = com.intellij.openapi.application.ex.ApplicationEx

suspend fun <T> span(
  name: String,
  context: CoroutineContext = EmptyCoroutineContext,
  action: suspend CoroutineScope.() -> T
): T {
  val namedContext = context + CoroutineName(name)
  return withContext(namedContext, action)
}
