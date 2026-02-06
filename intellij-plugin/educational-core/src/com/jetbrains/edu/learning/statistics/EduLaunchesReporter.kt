package com.jetbrains.edu.learning.statistics

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.JetBrainsPermanentInstallationID
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.HttpRequests
import com.jetbrains.edu.learning.courseFormat.Course
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


@Service(Service.Level.APP)
class EduLaunchesReporter(private val scope: CoroutineScope) {

  /**
   * Tracks if we are currently trying to make the necessary request.
   * Used to prevent concurrent requests
   */
  private val inProgressLock = Mutex()

  fun sendStats(course: Course) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return
    }

    val lastUpdate = PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0L)
    val shouldUpdate = lastUpdate == 0L || System.currentTimeMillis() - lastUpdate > TimeUnit.DAYS.toMillis(1)
    if (shouldUpdate) {
      val url = getUpdateUrl(course)
      trySendStats(url)
    }
  }

  private fun trySendStats(url: String) {
    scope.launch {
      if (inProgressLock.tryLock()) {
        try {
          val requestSuccessful = withRetry {
            withContext(Dispatchers.IO) {
              makeRequest(url)
            }
          }
          if (requestSuccessful) {
            PropertiesComponent.getInstance().setValue(LAST_UPDATE, System.currentTimeMillis().toString())
          }
        }
        finally {
          inProgressLock.unlock()
        }
      }
    }
  }

  private suspend fun withRetry(action: suspend () -> Boolean): Boolean {
    var attempts = 0
    var delay = BASE_DELAY
    while (true) {
      attempts++
      LOG.debug("Attempt $attempts")
      if (action()) {
        return true
      }
      // Check attempts here not to wait after the last attempt
      if (attempts > MAX_RETRY_ATTEMPTS) {
        return false
      }

      delay(delay)
      delay *= 2
    }
  }

  private fun makeRequest(url: String): Boolean {
    LOG.debug("Making request to $url")
    try {
      HttpRequests.request(url).connect {
        JDOMUtil.load(it.reader)
        LOG.info("updated: $url")
      }
      return true
    }
    catch (_: UnknownHostException) {
      // No internet connections, no need to log anything
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Throwable) {
      LOG.warn(e)
    }
    return false
  }

  @OptIn(IntellijInternalApi::class)
  private fun getUpdateUrl(course: Course): String {
    val applicationInfo = ApplicationInfoEx.getInstanceEx()
    val buildNumber = applicationInfo.build.asString()
    val plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!
    val pluginId = plugin.pluginId.idString
    @Suppress("UsagesOfObsoleteApi")
    val os = URLEncoder.encode("${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION}", Charsets.UTF_8)
    val uid = JetBrainsPermanentInstallationID.get()
    val baseUrl = "https://plugins.jetbrains.com/plugins/list"
    val projectType = course.itemType
    val role = if (course.isStudy) "student" else "teacher"
    val id = if (course.id != 0) "&courseId=${course.id}" else ""
    return "$baseUrl?pluginId=$pluginId&build=$buildNumber&pluginVersion=${plugin.version}&os=$os&uuid=$uid&projectType=$projectType&role=$role$id"
  }

  companion object {
    private const val LAST_UPDATE: String = "com.jetbrains.edu.LAST_UPDATE"
    private const val PLUGIN_ID: String = "com.jetbrains.edu"

    private const val MAX_RETRY_ATTEMPTS = 5
    private val BASE_DELAY = 2.seconds

    private val LOG = logger<EduLaunchesReporter>()

    fun getInstance(): EduLaunchesReporter = service()
  }
}
