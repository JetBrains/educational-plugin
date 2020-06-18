package com.jetbrains.edu.kotlin.twitter

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator
import com.jetbrains.edu.learning.twitter.TwitterSettings.Companion.getInstance
import com.jetbrains.edu.learning.twitter.TwitterUtils.TwitterDialogPanel

class KtTwitterConfigurator : TwitterPluginConfigurator {
  override fun askToTweet(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    if (course.name == "Kotlin Koans") {
      val settings = getInstance()
      return settings.askToTweet() &&
             solvedTask.status == Solved &&
             (statusBeforeCheck == Unchecked || statusBeforeCheck == Failed) &&
             calculateTaskNumber(solvedTask) % 8 == 0
    }
    return false
  }

  override fun getTweetDialogPanel(solvedTask: Task): TwitterDialogPanel? {
    return KtTwitterDialogPanel(solvedTask)
  }

  companion object {
    fun calculateTaskNumber(solvedTask: Task): Int {
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
