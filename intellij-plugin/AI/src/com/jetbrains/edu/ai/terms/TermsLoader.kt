package com.jetbrains.edu.ai.terms

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.terms.connector.TermsServiceConnector
import com.jetbrains.edu.ai.terms.settings.TheoryLookupSettings
import com.jetbrains.edu.ai.terms.updater.TermsUpdateChecker
import com.jetbrains.edu.ai.translation.ui.AITranslationNotification.ActionLabel
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.CourseTermsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class TermsLoader(private val project: Project, private val scope: CoroutineScope) {
  private val mutex = Mutex()

  init {
    scope.launch {
      val combinedStateFlow = combineState(
        scope,
        TheoryLookupSettings.getInstance().theoryLookupProperties,
        TranslationProjectSettings.getInstance(project).translationProperties
      )
      combinedStateFlow.collectLatest { (theoryLookupProperties, translationProperties) ->
        if (theoryLookupProperties?.isEnabled == false) return@collectLatest

        val course = project.course as? EduCourse ?: return@collectLatest

        val translationLanguage = translationProperties?.language
        val languageCode = translationLanguage?.code ?: course.languageCode
        if (languageCode != TranslationLanguage.ENGLISH.code) return@collectLatest // TODO(support other languages)

        if (TermsProjectSettings.areCourseTermsLoaded(project, languageCode)) return@collectLatest
        if (isRunning(project)) return@collectLatest
        fetchAndApplyTerms(course, languageCode)
        TermsUpdateChecker.getInstance(project).checkUpdate(course)
      }
    }
  }

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
    if (languageCode != TranslationLanguage.ENGLISH.code) {
      LOG.warn("Language $languageCode requested for theory lookup is not English")
      return
    }
    runInBackgroundExclusively(EduAIBundle.message("ai.terms.already.running")) {
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
      //TODO(add statistics (fetch started))
      val termsResponse = fetchTerms(course, languageCode).onError { error ->
        //TODO(add statistics (fetch failed))
        LOG.warn("Failed to fetch terms for ${course.name} in $languageCode: $error")
        return@withBackgroundProgress
      }
      termsProjectSettings.setTerms(termsResponse.toTermsProperties())
      //TODO(add statistics (fetch finished))
    }
  }

  private suspend fun doUpdateTerms(course: EduCourse, termsProperties: TermsProperties) {
    withBackgroundProgress(project, EduAIBundle.message("ai.terms.update.course.terms")) {
      val (language, _, version) = termsProperties
      //TODO(add statistics (update started))
      val termsResponse = fetchTerms(course, language).onError { error ->
        //TODO(add statistics (update failed))
        LOG.warn("Failed to update terms for ${course.name} in $language: $error")
        return@withBackgroundProgress
      }
      val termsProjectSettings = TermsProjectSettings.getInstance(project)
      if (version == termsResponse.termsVersion) {
        AITranslationNotificationManager.showInfoNotification(
          project,
          message = EduAIBundle.message("ai.terms.terms.is.up.to.date")
        )
        return@withBackgroundProgress
      }
      termsProjectSettings.setTerms(termsResponse.toTermsProperties())
      AITranslationNotificationManager.showInfoNotification(
        project,
        message = EduAIBundle.message("ai.terms.terms.has.been.updated")
      )
      //TODO(add statistics (update finished))
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
            // TODO(add statistics (fetch failed))
            fetchAndApplyTerms(course, languageCode)
          }
        )
        AITranslationNotificationManager.showErrorNotification(project, message = courseTerms.error.message(), actionLabel = actionLabel)
      }
      courseTerms
    }
  }

  private inline fun runInBackgroundExclusively(
    @NotificationContent lockNotAcquiredNotificationText: String,
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
        AITranslationNotificationManager.showErrorNotification(project, message = lockNotAcquiredNotificationText)
      }
    }
  }

  private suspend fun downloadTerms(course: EduCourse, languageCode: String): Result<CourseTermsResponse, AIServiceError> {
    if (languageCode != TranslationLanguage.ENGLISH.code) return Err(TermsError.LANGUAGE_NOT_SUPPORTED)
    return TermsServiceConnector.getInstance().getCourseTerms(
      course.id,
      course.marketplaceCourseVersion,
      TranslationLanguage.ENGLISH
    )
  }

  private fun CourseTermsResponse.toTermsProperties(): TermsProperties =
    TermsProperties(language.code, terms.mapKeys { it.key.toInt() }, termsVersion)

  private fun <T1, T2> combineState(
    scope: CoroutineScope,
    state1: StateFlow<T1>,
    state2: StateFlow<T2>,
  ): StateFlow<Pair<T1, T2>> = combine(state1, state2) { t1, t2 -> t1 to t2 }
    .stateIn(scope, SharingStarted.Eagerly, state1.value to state2.value)

  companion object {
    private val LOG = Logger.getInstance(TermsLoader::class.java)

    fun getInstance(project: Project): TermsLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).mutex.isLocked
  }
}