package com.jetbrains.edu.ai.translation

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.Companion.TASK_DESCRIPTION_PREFIX
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.translation.enum.Language
import com.jetbrains.educational.translation.format.CourseTranslation
import com.jetbrains.educational.translation.format.DescriptionText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Service(Service.Level.PROJECT)
class TranslationLoader(private val project: Project, private val scope: CoroutineScope) {
  @Suppress("MemberVisibilityCanBePrivate")
  fun loadAsync(course: EduCourse, language: Language) {
    scope.launch {
      load(course, language)
    }
  }

  @RequiresBlockingContext
  @RequiresEdt
  fun loadWithModalProgress(course: EduCourse, language: Language) {
    runWithModalProgressBlocking(project, EduAIBundle.message("ai.service.getting.course.translation")) {
      loadAsync(course, language)
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  suspend fun load(course: EduCourse, language: Language) {
    withContext(Dispatchers.IO) {
      doLoad(course, language)
    }
  }

  private suspend fun doLoad(course: EduCourse, language: Language) {
    val translation = downloadTranslation(course, language).onError { error ->
      LOG.error("Failed to download translation for ${course.name} to $language: $error")
      return
    }
    course.saveTranslation(translation)
  }

  private suspend fun downloadTranslation(course: EduCourse, language: Language): Result<CourseTranslation, String> {
    val marketplaceId = course.marketplaceId
    val updateVersion = course.updateVersion
    return TranslationServiceConnector.getInstance().getTranslatedCourse(marketplaceId, updateVersion, language)
  }

  private suspend fun EduCourse.saveTranslation(courseTranslation: CourseTranslation) {
    val taskDescriptions = courseTranslation.taskDescriptions
    for (task in allTasks) {
      val translation = taskDescriptions[task.taskEduId] ?: continue
      task.saveTranslation(translation)
    }
  }

  private suspend fun Task.saveTranslation(text: DescriptionText) {
    val taskDirectory = readAction { getTaskDirectory(project) } ?: return
    val name = "${TASK_DESCRIPTION_PREFIX}_${text.language.code}.${descriptionFormat.extension}"

    writeAction {
      try {
        GeneratorUtils.createTextChildFile(project, taskDirectory, name, text.text)
      }
      catch (exception: IOException) {
        LOG.error("Failed to write text to $taskDirectory: $exception")
      }
    }
  }

  companion object {
    private val LOG = thisLogger()

    fun getInstance(project: Project): TranslationLoader = project.service()
  }
}