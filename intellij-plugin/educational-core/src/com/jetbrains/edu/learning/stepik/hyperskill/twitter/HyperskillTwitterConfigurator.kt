package com.jetbrains.edu.learning.stepik.hyperskill.twitter

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator
import com.jetbrains.edu.learning.twitter.TwitterUtils
import java.nio.file.Path
import kotlin.random.Random

class HyperskillTwitterConfigurator : TwitterPluginConfigurator {

  override fun askToTweet(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    val course = project.course as? HyperskillCourse ?: return false
    if (!course.isStudy) return false
    if (statusBeforeCheck == CheckStatus.Solved) return false

    val projectLesson = course.getProjectLesson() ?: return false
    if (solvedTask.lesson != projectLesson) return false

    var allProjectTaskSolved = true
    projectLesson.visitTasks {
      allProjectTaskSolved = allProjectTaskSolved && it.status == CheckStatus.Solved
    }
    return allProjectTaskSolved
  }

  override fun getDefaultMessage(solvedTask: Task): String {
    val course = solvedTask.course
    val courseName = (course as? HyperskillCourse)?.getProjectLesson()?.presentableName ?: course.presentableName
    return EduCoreBundle.message("hyperskill.twitter.message", courseName)
  }

  override fun getImagePath(solvedTask: Task): Path? {
    val gifIndex = Random.Default.nextInt(NUMBER_OF_IMAGES)
    return TwitterUtils.pluginRelativePath("twitter/hyperskill/achievement$gifIndex.gif")
  }

  companion object {
    private const val NUMBER_OF_IMAGES = 2
  }
}
