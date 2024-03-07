package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.network.eduToolsUserAgent
import java.net.HttpURLConnection

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class HttpLink(link: String) : TaskDescriptionLink<String, String>(link) {
  override fun resolve(project: Project): String = link

  override fun open(project: Project, link: String) {
    EduBrowser.getInstance().browse(link)
  }

  override suspend fun validate(project: Project, link: String): String? {
    val delayStrategy = getDelayStrategy()
    var lastResult: String? = null
    repeat(MAX_ATTEMPTS) {
      val result = makeRequest()
      lastResult = result
      if (result == null) {
        return null
      }
      else {
        delayStrategy.delay(1000L)
      }
    }

    return lastResult
  }

  private fun makeRequest(): String? {
    return try {
      HttpRequests.request(link)
        .userAgent(eduToolsUserAgent)
        .throwStatusCodeException(false)
        .connect { request ->
          val connection = request.connection as HttpURLConnection
          val code = connection.responseCode
          if (code >= 400) {
            "Failed to resolve `$link` with $code code"
          }
          else {
            null
          }
        }
    }
    catch (e: Throwable) {
      e.message.orEmpty()
    }
  }

  companion object {
    private const val MAX_ATTEMPTS = 5
  }
}

private fun getDelayStrategy(): DelayStrategy {
  // We don't want to wait too long in tests
  return if (isUnitTestMode) FixedDelayStrategy(10) else BackoffDelayStrategy()
}

private interface DelayStrategy {
  suspend fun delay(timeMillis: Long)
}

private class FixedDelayStrategy(private val delayTime: Long) : DelayStrategy {
  override suspend fun delay(timeMillis: Long) {
    kotlinx.coroutines.delay(delayTime)
  }
}

private class BackoffDelayStrategy : DelayStrategy {
  @Volatile
  private var counter: Int = 0

  override suspend fun delay(timeMillis: Long) {
    val delayTime = timeMillis * (1 shl counter)
    counter += 1
    kotlinx.coroutines.delay(delayTime)
  }
}
