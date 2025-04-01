package com.jetbrains.edu.ai.terms.updater

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.terms.TERMS_NOTIFICATION_ENABLED_REGISTRY_ID
import com.jetbrains.edu.ai.terms.TERMS_NOTIFICATION_ID
import com.jetbrains.edu.ai.terms.TermsLoader
import com.jetbrains.edu.ai.terms.connector.TermsServiceConnector
import com.jetbrains.edu.ai.translation.isSameLanguage
import com.jetbrains.edu.learning.ai.terms.TheoryLookupSettings
import com.jetbrains.edu.learning.taskToolWindow.ui.notification.TaskToolWindowNotification.ActionLabel
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.ai.terms.TheoryLookupProperties
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.core.format.enum.TranslationLanguage

@Service(Service.Level.PROJECT)
class TermsUpdateChecker(private val project: Project) {
  suspend fun checkUpdate(course: EduCourse) {
    val theoryLookupProperties = TheoryLookupSettings.getInstance().theoryLookupProperties.value
    val translationProperties = TranslationProjectSettings.getInstance(project).translationProperties.value
    checkUpdate(course, theoryLookupProperties, translationProperties)
  }

  suspend fun checkUpdate(course: EduCourse, theoryLookupProperties: TheoryLookupProperties?, translationProperties: TranslationProperties?) {
    if (theoryLookupProperties?.isEnabled == false) return

    val translationLanguage = translationProperties?.language
    if (translationLanguage != null && !translationLanguage.isSameLanguage(course)) return

    val termsProperties = TermsProjectSettings.getInstance(project).termsProperties.value
    if (termsProperties == null) {
      val languageCode = course.languageCode
      TermsLoader.getInstance(project).fetchAndApplyTerms(course, languageCode)
      return
    }
    if (areTermsOutdated(course, termsProperties)) {
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
    if (!Registry.`is`(TERMS_NOTIFICATION_ENABLED_REGISTRY_ID, false)) {
      updateAction()
      return
    }
    val actionLabel = ActionLabel(
      name = EduCoreBundle.message("update.action"),
      action = updateAction
    )
    TaskToolWindowView.getInstance(project).showTaskDescriptionNotification(
      TERMS_NOTIFICATION_ID,
      EditorNotificationPanel.Status.Info,
      EduAIBundle.message("ai.terms.an.updated.version.of.the.terms.is.available"),
      actionLabel
    )
  }

  companion object {
    private val LOG = Logger.getInstance(TermsUpdateChecker::class.java)

    fun getInstance(project: Project): TermsUpdateChecker = project.service()
  }
}