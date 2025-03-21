package com.jetbrains.edu.ai.terms

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.CommonAIServiceError
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.terms.connector.TermsServiceConnector
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.learning.taskToolWindow.ui.notification.TaskToolWindowNotification.ActionLabel
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.CourseTermsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class TermsLoader(private val project: Project, private val scope: CoroutineScope) {
  private val mutex = Mutex()

  fun updateTerms(course: EduCourse, termsProperties: TermsProperties) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.update.is.not.possible")) {
      doUpdateTerms(course, termsProperties)
    }
  }

  fun updateTermsWhenCourseUpdate(course: EduCourse) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.update.is.not.possible")) {
      val termsProperties = TermsProjectSettings.getInstance(project).termsProperties.value
      doResetTerms()
      if (termsProperties != null) {
        doUpdateTerms(course, termsProperties)
      }
    }
  }

  fun fetchAndApplyTerms(course: EduCourse, languageCode: String) {
    if (languageCode != TranslationLanguage.ENGLISH.code || languageCode != course.languageCode) {
      LOG.warn("Language $languageCode requested for theory lookup is not English")
      return
    }
    runInBackgroundExclusively {
      doFetchAndApplyTerms(course, languageCode)
    }
  }

  fun resetTerms() {
    runInBackgroundExclusively(EduAIBundle.message("ai.terms.reset.is.not.possible")) {
      doResetTerms()
    }
  }

  private suspend fun doResetTerms() {
    if (TermsProjectSettings.areCourseTermsLoaded(project)) {
      withBackgroundProgress(project, EduAIBundle.message("ai.terms.reset.course.terms")) {
        TermsProjectSettings.getInstance(project).resetTerms()
      }
    }
  }

  private suspend fun doFetchAndApplyTerms(course: EduCourse, languageCode: String) {
    withBackgroundProgress(project, EduAIBundle.message("ai.terms.getting.course.terms")) {
      val termsProjectSettings = TermsProjectSettings.getInstance(project)
      val properties = termsProjectSettings.getTermsByLanguage(languageCode)
      if (properties != null) {
        termsProjectSettings.setTerms(properties)
        return@withBackgroundProgress
      }
      EduAIFeaturesCounterUsageCollector.theoryLookupStarted(course, languageCode)
      val termsResponse = fetchTerms(course, languageCode).onError { error ->
        EduAIFeaturesCounterUsageCollector.theoryLookupFinishedWithError(course, languageCode, error)
        LOG.warn("Failed to fetch terms for ${course.name} in $languageCode: $error")
        return@withBackgroundProgress
      }
      termsProjectSettings.setTerms(termsResponse.toTermsProperties())
      EduAIFeaturesCounterUsageCollector.theoryLookupFinishedSuccessfully(course, languageCode)
    }
  }

  private suspend fun doUpdateTerms(course: EduCourse, termsProperties: TermsProperties) {
    withBackgroundProgress(project, EduAIBundle.message("ai.terms.update.course.terms")) {
      val (language, _, version) = termsProperties
      val termsResponse = fetchTerms(course, language).onError { error ->
        EduAIFeaturesCounterUsageCollector.theoryLookupFinishedWithError(course, language, error)
        LOG.warn("Failed to update terms for ${course.name} in $language: $error")
        return@withBackgroundProgress
      }
      val termsProjectSettings = TermsProjectSettings.getInstance(project)
      if (version == termsResponse.termsVersion) {
        TaskToolWindowView.getInstance(project).showTaskDescriptionNotification(
          TERMS_NOTIFICATION_ID,
          EditorNotificationPanel.Status.Info,
          EduAIBundle.message("ai.terms.terms.is.up.to.date")
        )
        return@withBackgroundProgress
      }
      termsProjectSettings.setTerms(termsResponse.toTermsProperties())
      TaskToolWindowView.getInstance(project).showTaskDescriptionNotification(
        TERMS_NOTIFICATION_ID,
        EditorNotificationPanel.Status.Info,
        EduAIBundle.message("ai.terms.terms.has.been.updated")
      )
      EduAIFeaturesCounterUsageCollector.theoryLookupUpdated(course, language)
    }
  }

  private suspend fun fetchTerms(
    course: EduCourse,
    languageCode: String
  ): Result<CourseTermsResponse, AIServiceError> {
    return withContext(Dispatchers.IO) {
      val courseTerms = downloadTerms(course, languageCode)
      if (courseTerms is Err) {
        val actionLabel = ActionLabel(
          name = EduCoreBundle.message("retry"),
          action = {
            EduAIFeaturesCounterUsageCollector.theoryLookupRetried(course, languageCode, courseTerms.error)
            fetchAndApplyTerms(course, languageCode)
          }
        )
        when (courseTerms.error) {
          is TermsError -> {
            TaskToolWindowView.getInstance(project).showTaskDescriptionNotification(
              TERMS_NOTIFICATION_ID,
              EditorNotificationPanel.Status.Error,
              courseTerms.error.message(),
              actionLabel
            )
          }
          // do not override notification if there exist any notification produced by a common error
          // note that corresponding notification produced by common error in translation loader does override previous
          // because after translation retry it will be applied to translation project settings
          // and therefore terms observer will try to update the terms after that
          // TODO(it's a hack, so it should be refactored when we merge api of translation and terms)
          is CommonAIServiceError -> {
            TaskToolWindowView.getInstance(project).showTaskDescriptionNotificationIfAbsent(
              CommonAIServiceError.NOTIFICATION_ID,
              EditorNotificationPanel.Status.Error,
              courseTerms.error.message(),
              actionLabel
            )
          }
        }
      }
      courseTerms
    }
  }

  private inline fun runInBackgroundExclusively(
    @NotificationContent lockNotAcquiredNotificationText: String? = null,
    crossinline action: suspend () -> Unit
  ) {
    scope.launch {
      if (mutex.tryLock()) {
        try {
          action()
        }
        finally {
          mutex.unlock()
        }
      }
      else {
        if (lockNotAcquiredNotificationText != null) {
          TaskToolWindowView.getInstance(project).showTaskDescriptionNotification(
            TERMS_NOTIFICATION_ID,
            EditorNotificationPanel.Status.Error,
            lockNotAcquiredNotificationText
          )
        }
      }
    }
  }

  private suspend fun downloadTerms(course: EduCourse, languageCode: String): Result<CourseTermsResponse, AIServiceError> {
    if (TranslationLanguage.findByCode(languageCode) != TranslationLanguage.ENGLISH || languageCode != course.languageCode) {
      return Err(TermsError.LANGUAGE_NOT_SUPPORTED)
    }
    return TermsServiceConnector.getInstance().getCourseTerms(
      course.id,
      course.marketplaceCourseVersion,
      TranslationLanguage.ENGLISH
    )
  }

  private fun CourseTermsResponse.toTermsProperties(): TermsProperties =
    TermsProperties(language.code, terms.mapKeys { it.key.toInt() }, termsVersion)

  companion object {
    private val LOG = Logger.getInstance(TermsLoader::class.java)

    fun getInstance(project: Project): TermsLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).mutex.isLocked
  }
}