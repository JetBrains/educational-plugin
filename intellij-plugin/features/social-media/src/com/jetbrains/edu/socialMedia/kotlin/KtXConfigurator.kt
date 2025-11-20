package com.jetbrains.edu.socialMedia.kotlin

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.SocialMediaUtils
import com.jetbrains.edu.socialMedia.x.XPluginConfigurator
import org.jetbrains.annotations.NonNls
import java.nio.file.Path

class KtXConfigurator : XPluginConfigurator {

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

  override fun getIndexWithImagePath(solvedTask: Task, imageIndex: Int?): Pair<Int, Path?> {
    val solvedTaskNumber = imageIndex ?: calculateTaskNumber(solvedTask)
    val level = solvedTaskNumber / 8
    val imagePath = SocialMediaUtils.pluginRelativePath("socialMedia/x/kotlin_koans/images/${level}level.gif")
    return level to imagePath
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
