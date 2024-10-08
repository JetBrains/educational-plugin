package com.jetbrains.edu.kotlin.twitter

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings
import com.jetbrains.edu.learning.socialmedia.SocialmediaUtils
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterPluginConfigurator
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterSettings
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterUtils
import org.jetbrains.annotations.NonNls
import java.nio.file.Path

class KtTwitterConfigurator : TwitterPluginConfigurator {

  override val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState> = TwitterSettings.getInstance()

  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    if (course.name == "Kotlin Koans") {
      return solvedTask.status == Solved &&
             (statusBeforeCheck == Unchecked || statusBeforeCheck == Failed) &&
             calculateTaskNumber(solvedTask) % 8 == 0
    }
    return false
  }

  override fun getMessage(solvedTask: Task): String {
    val solvedTaskNumber = calculateTaskNumber(solvedTask)
    return String.format(COMPLETE_KOTLIN_KOANS_LEVEL, solvedTaskNumber / 8)
  }

  override fun getIndexWithImagePath(solvedTask: Task?, imageIndex: Int?): Pair<Int, Path?> {
    solvedTask ?: error("Task is not provided")
    val solvedTaskNumber = imageIndex ?: calculateTaskNumber(solvedTask)
    val level = solvedTaskNumber / 8
    val imagePath = SocialmediaUtils.pluginRelativePath("socialmedia/twitter/kotlin_koans/images/${level}level.gif")
    return level to imagePath
  }

  override fun doPost(solvedTask: Task, imageIndex: Int?) {
    if (!settings.askToPost) return
    val (_, imagePath) = getIndexWithImagePath(solvedTask, imageIndex)
    TwitterUtils.doPost(getMessage(solvedTask), imagePath)
  }

  companion object {

    @NonNls
    private const val COMPLETE_KOTLIN_KOANS_LEVEL =
      "Hey, I just completed level %d of Kotlin Koans. https://kotlinlang.org/docs/tutorials/koans.html #kotlinkoans"

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
