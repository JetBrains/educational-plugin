package com.jetbrains.edu.ai.translation

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
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
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
  @RequiresBlockingContext
  @RequiresEdt
  fun loadAndSaveWithModalProgress(course: EduCourse, language: Language) {
    runWithModalProgressBlocking(project, EduAIBundle.message("ai.service.getting.course.translation")) {
      loadAndSave(course, language)
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun loadAndSave(course: EduCourse, language: Language) {
    scope.launch {
      loadAndSaveAsync(course, language)
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  suspend fun loadAndSaveAsync(course: EduCourse, language: Language) {
    withContext(Dispatchers.IO) {
      val translation = downloadTranslation(course, language).onError { error ->
        LOG.error("Failed to download translation for ${course.name} to $language: $error")
        return@withContext
      } ?: return@withContext
      course.saveTranslation(translation)
    }
  }

  private suspend fun downloadTranslation(course: EduCourse, language: Language): Result<CourseTranslation?, String> {
    val marketplaceId = course.marketplaceId
    val updateVersion = course.updateVersion
    return TranslationServiceConnector.getInstance().getTranslatedCourse(marketplaceId, updateVersion, language)
  }

  private suspend fun EduCourse.saveTranslation(courseTranslation: CourseTranslation) {
    val taskDescriptions = courseTranslation.taskDescriptions
    writeAction {
      for (task in allTasks) {
        val translation = taskDescriptions[task.taskEduId] ?: continue
        task.saveTranslation(translation)
      }

      translatedToLanguageCode = courseTranslation.language.code
      YamlFormatSynchronizer.saveItem(this)
    }
  }

  @RequiresBlockingContext
  private fun Task.saveTranslation(text: DescriptionText) {
    val taskDirectory = getTaskDirectory(project) ?: return
    val name = descriptionFormat.fileNameWithTranslation(text.language.code)

    try {
      GeneratorUtils.createTextChildFile(project, taskDirectory, name, text.text)
    }
    catch (exception: IOException) {
      LOG.error("Failed to write text to $taskDirectory: $exception")
    }
  }

  companion object {
    private val LOG = thisLogger()

    fun getInstance(project: Project): TranslationLoader = project.service()
  }
}