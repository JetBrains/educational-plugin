package com.jetbrains.edu.learning

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Helps test code that runs asynchronous code with CompletableFutures.
 * Such code is problematic for tests because there is no way in tests to wait until all asynchronous tasks are finished.
 */
class FutureTracker {

  internal val pendingFutures = ConcurrentHashMap.newKeySet<CompletableFuture<*>>()

  fun track(future: CompletableFuture<*>) {
    if (isUnitTestMode) {
      pendingFutures.add(future)
      future.whenComplete { _, _ -> pendingFutures.remove(future) }
    }
  }

  fun waitForPendingFuturesInTests() {
    if (isUnitTestMode) {
      CompletableFuture.allOf(*pendingFutures.toTypedArray()).get(30, TimeUnit.SECONDS)
    }
  }
}

fun <T> CompletableFuture<T>.tracked(futureTracker: FutureTracker): CompletableFuture<T> {
  if (isUnitTestMode) {
    futureTracker.track(this)
  }
  return this
}