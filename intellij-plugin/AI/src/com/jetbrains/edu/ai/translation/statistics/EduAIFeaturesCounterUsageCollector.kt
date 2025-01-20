package com.jetbrains.edu.ai.translation.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.jetbrains.edu.ai.translation.TranslationError
import com.jetbrains.edu.ai.translation.settings.TranslationSettings
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.ALWAYS_TRANSLATE_FIELD
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.HINTS_BANNER_TYPE_FIELD
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.ORIGINAL_LANG_FIELD
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.TRANSLATION_ERROR_FIELD
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.TRANSLATION_LANG_FIELD
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_ID_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_UPDATE_VERSION_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.TASK_ID_FIELD
import com.jetbrains.edu.learning.statistics.EventLogGroup
import com.jetbrains.edu.learning.statistics.registerEvent
import com.jetbrains.educational.core.format.enum.TranslationLanguage

class EduAIFeaturesCounterUsageCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  @Suppress("CompanionObjectInExtension")
  companion object {
    private val GROUP = EventLogGroup(
      "educational.ai.features",
      "The metric is reported in case a user has called the corresponding JetBrains Academy AI features.",
      2,
    )

    private val TRANSLATION_BUTTON_CLICKED_EVENT = GROUP.registerEvent(
      "translation.button.clicked",
      "The event is recorded when the translation button is clicked.",
      COURSE_ID_FIELD
    )
    private val TRANSLATION_DISABLED_EVENT = GROUP.registerEvent(
      "translation.disabled",
      "The event is recorded when the course translation is disabled.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
    )
    private val TRANSLATION_FINISHED_EVENT = GROUP.registerVarargEvent(
      "translation.finished",
      "The event is recorded when the translation is successfully completed.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      ALWAYS_TRANSLATE_FIELD
    )
    private val TRANSLATION_FINISHED_WITH_ERROR_EVENT = GROUP.registerVarargEvent(
      "translation.finished.with.error",
      "The event is recorded in case an error occurs during translation.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      TRANSLATION_ERROR_FIELD
    )
    private val TRANSLATION_LANGUAGE_PICKER_OPENED_EVENT = GROUP.registerEvent(
      "translation.language.picker.opened",
      "The event is recorded when the language picker is opened by the user.",
      COURSE_ID_FIELD
    )
    private val TRANSLATION_RETRIED_EVENT = GROUP.registerVarargEvent(
      "translation.retried",
      "The event is recorded when a retry of the translation process is initiated.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      TRANSLATION_ERROR_FIELD,
    )
    private val TRANSLATION_STARTED_EVENT = GROUP.registerVarargEvent(
      "translation.started",
      "The event is recorded when the translation process starts.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      ALWAYS_TRANSLATE_FIELD,
    )
    private val TRANSLATION_UPDATED_EVENT = GROUP.registerEvent(
      "translation.updated",
      "The event is recorded when the translation is successfully updated.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
    )

    private val HINTS_GET_HINT_BUTTON_CLICKED_EVENT = GROUP.registerEvent(
      "hints.get.hint.button.clicked",
      "The event is recorded when `Get Hint` button is clicked.",
      COURSE_ID_FIELD,
      COURSE_UPDATE_VERSION_FIELD,
      TASK_ID_FIELD,
    )
    private val HINTS_HINT_BANNER_SHOWN_EVENT = GROUP.registerVarargEvent(
      "hints.hint.banner.shown",
      "Represents the type of banner that is shown to the user.",
      HINTS_BANNER_TYPE_FIELD,
      COURSE_ID_FIELD,
      COURSE_UPDATE_VERSION_FIELD,
      TASK_ID_FIELD,
    )
    private val HINTS_HINT_BANNER_CLOSED_EVENT = GROUP.registerEvent(
      "hints.hint.banner.closed",
      "The event is recorded when the hint banner is closed by user.",
      COURSE_ID_FIELD,
      COURSE_UPDATE_VERSION_FIELD,
      TASK_ID_FIELD,
    )
    private val HINTS_SHOW_IN_CODE_CLICKED_EVENT = GROUP.registerEvent(
      "hints.show.in.code.clicked",
      "The event is recorded `Show in code` link is clicked from the CodeHint banner.",
      COURSE_ID_FIELD,
      COURSE_UPDATE_VERSION_FIELD,
      TASK_ID_FIELD,
    )
    private val HINTS_RETRY_CLICKED_EVENT = GROUP.registerEvent(
      "hints.retry.clicked",
      "The event is recorded when the `Retry` action link is clicked.",
      COURSE_ID_FIELD,
      COURSE_UPDATE_VERSION_FIELD,
      TASK_ID_FIELD,
    )
    private val HINTS_CODE_HINT_ACCEPTED_EVENT = GROUP.registerEvent(
      "hints.code.hint.accepted",
      "The event is recorded in case the shown CodeHint was accepted.",
      COURSE_ID_FIELD,
      COURSE_UPDATE_VERSION_FIELD,
      TASK_ID_FIELD,
    )
    private val HINTS_CODE_HINT_CANCELLED_EVENT = GROUP.registerEvent(
      "hints.code.hint.cancelled",
      "The event is recorded in case the shown CodeHint was cancelled.",
      COURSE_ID_FIELD,
      COURSE_UPDATE_VERSION_FIELD,
      TASK_ID_FIELD,
    )

    fun translationButtonClicked(course: EduCourse) = TRANSLATION_BUTTON_CLICKED_EVENT.log(course.id)

    fun translationDisabled(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_DISABLED_EVENT.log(course.id, course.languageCode, translationLanguage)

    fun translationFinishedSuccessfully(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_FINISHED_EVENT.log(
        COURSE_ID_FIELD.with(course.id),
        ORIGINAL_LANG_FIELD.with(course.languageCode),
        TRANSLATION_LANG_FIELD.with(translationLanguage),
        alwaysTranslate()
      )

    fun translationFinishedWithError(
      course: EduCourse,
      translationLanguage: TranslationLanguage,
      translationError: TranslationError
    ) = TRANSLATION_FINISHED_WITH_ERROR_EVENT.log(
      COURSE_ID_FIELD.with(course.id),
      ORIGINAL_LANG_FIELD.with(course.languageCode),
      TRANSLATION_LANG_FIELD.with(translationLanguage),
      TRANSLATION_ERROR_FIELD.with(translationError)
    )

    fun translationLanguagePickerOpened(course: EduCourse) = TRANSLATION_LANGUAGE_PICKER_OPENED_EVENT.log(course.id)

    fun translationRetried(course: EduCourse, translationLanguage: TranslationLanguage, translationError: TranslationError) =
      TRANSLATION_RETRIED_EVENT.log(
        COURSE_ID_FIELD.with(course.id),
        ORIGINAL_LANG_FIELD.with(course.languageCode),
        TRANSLATION_LANG_FIELD.with(translationLanguage),
        TRANSLATION_ERROR_FIELD.with(translationError),
      )

    fun translationStarted(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_STARTED_EVENT.log(
        COURSE_ID_FIELD.with(course.id),
        ORIGINAL_LANG_FIELD.with(course.languageCode),
        TRANSLATION_LANG_FIELD.with(translationLanguage),
        alwaysTranslate(),
      )

    fun translationUpdated(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_UPDATED_EVENT.log(course.id, course.languageCode, translationLanguage)

    private fun alwaysTranslate(): EventPair<Boolean> {
      val autoTranslate = TranslationSettings.getInstance().autoTranslate
      return ALWAYS_TRANSLATE_FIELD.with(autoTranslate)
    }

    fun hintButtonClicked(task: Task) =
      HINTS_GET_HINT_BUTTON_CLICKED_EVENT.log(task.course.id, task.course.marketplaceCourseVersion, task.id)

    fun hintBannerShown(type: EduAIFeaturesEventFields.HintBannerType, task: Task) =
      HINTS_HINT_BANNER_SHOWN_EVENT.log(
        HINTS_BANNER_TYPE_FIELD.with(type),
        COURSE_ID_FIELD.with(task.course.id),
        COURSE_UPDATE_VERSION_FIELD.with(task.course.marketplaceCourseVersion),
        TASK_ID_FIELD.with(task.id)
      )

    fun hintBannerClosed(task: Task) = HINTS_HINT_BANNER_CLOSED_EVENT.log(task.course.id, task.course.marketplaceCourseVersion, task.id)

    fun hintShowInCodeClicked(task: Task) =
      HINTS_SHOW_IN_CODE_CLICKED_EVENT.log(task.course.id, task.course.marketplaceCourseVersion, task.id)

    fun hintRetryClicked(task: Task) = HINTS_RETRY_CLICKED_EVENT.log(task.course.id, task.course.marketplaceCourseVersion, task.id)

    fun codeHintAccepted(task: Task) = HINTS_CODE_HINT_ACCEPTED_EVENT.log(task.course.id, task.course.marketplaceCourseVersion, task.id)

    fun codeHintCancelled(task: Task) = HINTS_CODE_HINT_CANCELLED_EVENT.log(task.course.id, task.course.marketplaceCourseVersion, task.id)
  }
}