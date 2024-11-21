package com.jetbrains.edu.ai.translation

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.application
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.ai.translation.settings.translationSettings
import com.jetbrains.edu.ai.translation.ui.AITranslationNotification.ActionLabel
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.ai.translationSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.fileNameWithTranslation
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.CourseTranslationResponse
import com.jetbrains.educational.translation.format.TranslatedText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import java.io.IOException

@Service(Service.Level.PROJECT)
class TranslationLoader(private val project: Project, private val scope: CoroutineScope) {
  private val mutex = Mutex()

  init {
    scope.launch {
      application.translationSettings().autoTranslationSettings.collectLatest { properties ->
        if (properties == null) return@collectLatest
        val course = project.course as? EduCourse ?: return@collectLatest
        if (TranslationProjectSettings.isCourseTranslated(project)) return@collectLatest
        if (isRunning(project)) return@collectLatest
        if (properties.autoTranslate) {
          fetchAndApplyTranslation(course, properties.language)
        }
      }
    }
  }

  fun fetchAndApplyTranslation(course: EduCourse, translationLanguage: TranslationLanguage) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.already.running")) {
      withBackgroundProgress(project, EduAIBundle.message("ai.translation.getting.course.translation")) {
        val translationSettings = project.translationSettings()

        if (course.isTranslationExists(translationLanguage)) {
          val properties = translationSettings.getTranslationPropertiesByLanguage(translationLanguage)
          if (properties != null) {
            translationSettings.setTranslation(properties)
            return@withBackgroundProgress
          }
        }

        val translation = fetchTranslation(course, translationLanguage)
        course.saveTranslation(translation)
        translationSettings.setTranslation(translation.toTranslationProperties())
      }
    }
  }

  fun updateTranslation(course: EduCourse, translationProperties: TranslationProperties) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.already.running")) {
      withBackgroundProgress(project, EduAIBundle.message("ai.translation.update.course.translation")) {
        val (language, _, version) = translationProperties
        val translation = fetchTranslation(course, language)
        val translationSettings = project.translationSettings()
        if (version == translation.version) {
          AITranslationNotificationManager.showInfoNotification(
            project,
            message = EduAIBundle.message("ai.translation.translation.is.up.to.date")
          )
          return@withBackgroundProgress
        }
        course.saveTranslation(translation)
        translationSettings.setTranslation(translation.toTranslationProperties())
        AITranslationNotificationManager.showInfoNotification(
          project,
          message = EduAIBundle.message("ai.translation.translation.has.been.updated")
        )
      }
    }
  }

  fun resetCourseTranslation(course: EduCourse) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.reset.is.not.possible")) {
      if (TranslationProjectSettings.isCourseTranslated(project)) {
        withBackgroundProgress(project, EduAIBundle.message("ai.translation.reset.course.translation")) {
          TranslationProjectSettings.resetTranslation(project)
        }
      }
      withBackgroundProgress(project, EduAIBundle.message("ai.translation.deleting.course.translation.files")) {
        course.deleteAllTranslations()
      }
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

  private suspend fun fetchTranslation(course: EduCourse, language: TranslationLanguage): CourseTranslationResponse =
    withContext(Dispatchers.IO) {
      downloadTranslation(course, language).onError { error ->
        val actionLabel = ActionLabel(
          name = EduCoreBundle.message("retry"),
          action = { fetchAndApplyTranslation(course, language) }
        )
        AITranslationNotificationManager.showErrorNotification(project, message = error, actionLabel = actionLabel)
        error("Failed to download translation for ${course.name} to $language: $error")
      }
    }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun EduCourse.deleteAllTranslations() = allTasks.map {
    scope.async(Dispatchers.IO) {
      TranslationLanguage.entries.forEach { language ->
        it.deleteTranslation(language)
      }
    }
  }.awaitAll()

  private suspend fun EduCourse.isTranslationExists(translationLanguage: TranslationLanguage): Boolean =
    readAction {
      allTasks.all {
        it.getDescriptionFile(project, translationLanguage = translationLanguage)?.exists() == true
      }
    }

  private suspend fun downloadTranslation(course: EduCourse, language: TranslationLanguage): Result<CourseTranslationResponse, String> {
    val translation = TranslationServiceConnector.getInstance().getTranslatedCourse(course.id, course.marketplaceCourseVersion, language)
      .onError { error -> return Err(error) }
    if (translation != null) {
      return Ok(translation)
    }
    return Err(EduAIBundle.message("ai.translation.course.translation.does.not.exist"))
  }

  private suspend fun EduCourse.saveTranslation(courseTranslation: CourseTranslationResponse) {
    val taskDescriptions = courseTranslation.taskDescriptions
    writeAction {
      for (task in allTasks) {
        val translation = taskDescriptions[task.id.toString()] ?: continue
        task.saveTranslation(translation)
      }
    }
  }

  @RequiresBlockingContext
  private fun Task.saveTranslation(text: TranslatedText) {
    val taskDirectory = getTaskDirectory(project) ?: return
    val name = descriptionFormat.fileNameWithTranslation(text.language)

    try {
      GeneratorUtils.createTextChildFile(project, taskDirectory, name, text.text)
    }
    catch (exception: IOException) {
      LOG.error("Failed to write text to $taskDirectory", exception)
      throw exception
    }
  }

  private suspend fun Task.deleteTranslation(translationLanguage: TranslationLanguage) {
    try {
      val translationFile = readAction {
        getDescriptionFile(project, translationLanguage = translationLanguage)
      } ?: return
      writeAction {
        translationFile.delete(this::class.java)
      }
    }
    catch (exception: IOException) {
      LOG.error("Failed to delete ${translationLanguage.label} translation file", exception)
      throw exception
    }
  }

  private fun CourseTranslationResponse.toTranslationProperties(): TranslationProperties =
    TranslationProperties(language, itemNames, version)

  companion object {
    private val LOG = Logger.getInstance(TranslationLoader::class.java)

    fun getInstance(project: Project): TranslationLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).mutex.isLocked
  }
}