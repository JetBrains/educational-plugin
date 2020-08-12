package com.jetbrains.edu.kotlin.twitter

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator
import com.jetbrains.edu.learning.twitter.TwitterSettings
import org.jetbrains.annotations.NonNls
import java.nio.file.Path

class KtTwitterConfigurator : TwitterPluginConfigurator {
  override fun askToTweet(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    if (course.name == "Kotlin Koans") {
      val settings = TwitterSettings.getInstance()
      return settings.askToTweet() &&
             solvedTask.status == Solved &&
             (statusBeforeCheck == Unchecked || statusBeforeCheck == Failed) &&
             calculateTaskNumber(solvedTask) % 8 == 0
    }
    return false
  }

  override fun getDefaultMessage(solvedTask: Task): String {
    val solvedTaskNumber = calculateTaskNumber(solvedTask)
    return String.format(COMPLETE_KOTLIN_KOANS_LEVEL, solvedTaskNumber / 8)
  }

  override fun getImagePath(solvedTask: Task): Path? {
    // BACKCOMPAT: 2019.3. Use `pluginPath` instead of `path?.toPath()`
    @Suppress("DEPRECATION")
    val path = PluginManagerCore.getPlugin(PluginId.getId(EduNames.PLUGIN_ID))?.path?.toPath() ?: return null

    val solvedTaskNumber = calculateTaskNumber(solvedTask)
    val level = solvedTaskNumber / 8
    return path.resolve("twitter/kotlin_koans/images/${level}level.gif").takeIf { it.exists() }
  }

  companion object {

    @NonNls
    private val COMPLETE_KOTLIN_KOANS_LEVEL = "Hey, I just completed level %d of Kotlin Koans. https://kotlinlang.org/docs/tutorials/koans.html #kotlinkoans"

    private fun calculateTaskNumber(solvedTask: Task): Int {
      val lesson = solvedTask.lesson
      val course = lesson.course
      var solvedTaskNumber = 0
      for (currentLesson in course.lessons) {
        for (task in currentLesson.taskList) {
          if (task.status == Solved) {
            solvedTaskNumber++
          }
        }
      }
      return solvedTaskNumber
    }
  }
}
