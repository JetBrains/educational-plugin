package com.jetbrains.edu.learning.newproject.environment

import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder

/**
 * Runs [collectData] and saves result in [cache] under [key].
 * If the result is already cached, returns the cached value
 */
context(cache: UserDataHolder)
suspend fun <R : Any> withCaching(key: Key<R>, collectData: suspend () -> R): R {
  val cached = cache.getUserData(key)
  if (cached != null) return cached
  fileLogger().info("Collecting data for `$key` key")
  val result = collectData()
  cache.putUserData(key, result)
  return result
}
