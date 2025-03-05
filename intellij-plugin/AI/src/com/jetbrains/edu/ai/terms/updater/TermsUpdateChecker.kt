package com.jetbrains.edu.ai.terms.updater

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.terms.TermsLoader
import com.jetbrains.edu.ai.terms.connector.TermsServiceConnector
import com.jetbrains.edu.ai.terms.settings.TheoryLookupSettings
import com.jetbrains.edu.ai.terms.ui.AITermsNotificationManager
import com.jetbrains.edu.ai.ui.AINotification.ActionLabel
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.format.enum.TranslationLanguage

@Service(Service.Level.PROJECT)
class TermsUpdateChecker(private val project: Project) {
  suspend fun checkUpdate(course: EduCourse) {
    val theoryLookupSettings = TheoryLookupSettings.getInstance()
    if (!theoryLookupSettings.isTheoryLookupEnabled) return
    val termsProperties = TermsProjectSettings.getInstance(project).termsProperties.value
    if (termsProperties != null && areTermsOutdated(course, termsProperties)) {
      showUpdateAvailableNotification {
        TermsLoader.getInstance(project).updateTerms(course, termsProperties)
      }
    }
  }

  private suspend fun areTermsOutdated(course: EduCourse, termsProperties: TermsProperties): Boolean {
    val (language, _, version) = termsProperties
    if (language != TranslationLanguage.ENGLISH.code) return false
    val latestVersion = TermsServiceConnector.getInstance()
      .getLatestTermsVersion(course.id, course.marketplaceCourseVersion, TranslationLanguage.ENGLISH)
      .onError {
        LOG.error(it.message())
        return false
      }
    return version != latestVersion
  }

  private fun showUpdateAvailableNotification(updateAction: () -> Unit) {
    val actionLabel = ActionLabel(
      name = EduCoreBundle.message("update.action"),
      action = updateAction
    )
    AITermsNotificationManager.getInstance(project).showInfoNotification(
      message = EduAIBundle.message("ai.terms.an.updated.version.of.the.terms.is.available"),
      actionLabel = actionLabel
    )
  }

  companion object {
    private val LOG = Logger.getInstance(TermsUpdateChecker::class.java)

    fun getInstance(project: Project): TermsUpdateChecker = project.service()
  }
}