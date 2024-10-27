package com.jetbrains.edu.ai.translation

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.translationSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.fileNameWithTranslation
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.enum.Language
import com.jetbrains.educational.translation.format.CourseTranslation
import com.jetbrains.educational.translation.format.DescriptionText
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class TranslationLoader(private val project: Project, private val scope: CoroutineScope) {
  private val lock = AtomicBoolean(false)

  private val isLocked: Boolean
    get() = lock.get()

  private fun lock(): Boolean {
    return lock.compareAndSet(false, true)
  }

  private fun unlock() {
    lock.set(false)
  }

  fun fetchAndApplyTranslation(course: EduCourse, language: Language) {
    scope.launch {
      try {
        if (!lock()) {
          EduNotificationManager.showErrorNotification(
            project,
            content = EduAIBundle.message("ai.translation.already.running")
          )
          return@launch
        }
        withBackgroundProgress(project, EduAIBundle.message("ai.translation.getting.course.translation")) {
          if (!course.isTranslationExists(language)) {
            val translation = fetchTranslation(course, language)
            course.saveTranslation(translation)
          }
          project.translationSettings().setCurrentTranslationLanguage(language)
        }
      }
      finally {
        unlock()
      }
    }
  }

  fun resetCourseTranslation(course: EduCourse) {
    scope.launch {
      try {
        if (!lock()) {
          EduNotificationManager.showErrorNotification(
            project,
            content = EduAIBundle.message("ai.translation.reset.is.not.possible")
          )
          return@launch
        }
        if (TranslationProjectSettings.getCurrentTranslationLanguage(project) != null) {
          withBackgroundProgress(project, EduAIBundle.message("ai.translation.reset.course.translation")) {
            TranslationProjectSettings.resetTranslation(project)
          }
        }
        withBackgroundProgress(project, EduAIBundle.message("ai.translation.deleting.course.translation.files")) {
          course.deleteAllTranslations()
        }
      }
      finally {
        unlock()
      }
    }
  }

  private suspend fun fetchTranslation(course: EduCourse, language: Language): CourseTranslation =
    withContext(Dispatchers.IO) {
      downloadTranslation(course, language).onError { error ->
        EduNotificationManager.showErrorNotification(project, content = error)
        error("Failed to download translation for ${course.name} to $language: $error")
      }
    }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun EduCourse.deleteAllTranslations() = allTasks.map {
    scope.async(Dispatchers.IO) {
      Language.entries.forEach { language ->
        it.deleteTranslation(language)
      }
    }
  }.awaitAll()

  private suspend fun EduCourse.isTranslationExists(language: Language): Boolean =
    readAction {
      allTasks.all {
        it.getDescriptionFile(project, translationLanguage = language)?.exists() == true
      }
    }

  private suspend fun downloadTranslation(course: EduCourse, language: Language): Result<CourseTranslation, String> {
    val marketplaceId = course.marketplaceId
    val updateVersion = course.updateVersion

    val translation = TranslationServiceConnector.getInstance().getTranslatedCourse(marketplaceId, updateVersion, language)
      .onError { error -> return Err(error) }
    if (translation != null) {
      return Ok(translation)
    }
    return Err("Translation does not exist")
  }

  private suspend fun EduCourse.saveTranslation(courseTranslation: CourseTranslation) {
    val taskDescriptions = courseTranslation.taskDescriptions
    writeAction {
      for (task in allTasks) {
        val translation = taskDescriptions[task.taskEduId] ?: continue
        task.saveTranslation(translation)
      }
    }
  }

  @RequiresBlockingContext
  private fun Task.saveTranslation(text: DescriptionText) {
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

  private suspend fun Task.deleteTranslation(language: Language) {
    try {
      val translationFile = readAction {
        getDescriptionFile(project, translationLanguage = language)
      } ?: return
      writeAction {
        translationFile.delete(this::class.java)
      }
    }
    catch (exception: IOException) {
      LOG.error("Failed to delete ${language.label} translation file", exception)
      throw exception
    }
  }

  companion object {
    private val LOG = thisLogger()

    fun getInstance(project: Project): TranslationLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).isLocked
  }
}