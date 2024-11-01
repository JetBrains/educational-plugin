package com.jetbrains.edu.ai.translation.updater

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.ai.translation.marketplaceId
import com.jetbrains.edu.ai.translation.updateVersion
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.ai.translationSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.onError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class TranslationUpdateChecker(private val project: Project, private val scope: CoroutineScope) {
  private val lock = AtomicBoolean(false)

  private val isRunning: Boolean
    get() = lock.get()

  private val checkInterval: Duration
    get() {
      // Default value is 3600 seconds (1 hour), set in AI.xml
      return Registry.intValue(REGISTRY_KEY).seconds
    }

  private fun launch(course: EduCourse) {
    if (lock.compareAndSet(false, true)) {
      scope.launch {
        while (true) {
          val translationProperties = project.translationSettings().translationProperties.value
          if (translationProperties != null && isTranslationOutdated(course, translationProperties)) {
            showUpdateAvailableNotification {
              TranslationLoader.getInstance(project).updateTranslation(course, translationProperties)
            }
          }
          delay(checkInterval)
        }
      }
    }
    else {
      LOG.error("Translation update checker is already running")
    }
  }

  private suspend fun isTranslationOutdated(course: EduCourse, translationProperties: TranslationProperties): Boolean {
    val (language, _, version) = translationProperties
    val latestVersion = TranslationServiceConnector.getInstance()
      .getLatestTranslationVersion(course.marketplaceId, course.updateVersion, language)
      .onError {
        LOG.error(it)
        return false
      }
    return version != latestVersion
  }

  private fun showUpdateAvailableNotification(updateAction: () -> Unit) {
    EduNotificationManager
      .create(INFORMATION, content = EduAIBundle.message("ai.translation.an.updated.version.of.the.translation.is.available"))
      .addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("update.action")) {
        updateAction()
      })
      .notify(project)
  }

  companion object {
    private val LOG = Logger.getInstance(TranslationUpdateChecker::class.java)

    private const val REGISTRY_KEY: String = "edu.ai.translation.update.check.interval"

    fun Project.launchTranslationUpdateChecker(course: EduCourse) {
      val checker = service<TranslationUpdateChecker>()
      if (!checker.isRunning) {
        checker.launch(course)
      }
    }
  }
}