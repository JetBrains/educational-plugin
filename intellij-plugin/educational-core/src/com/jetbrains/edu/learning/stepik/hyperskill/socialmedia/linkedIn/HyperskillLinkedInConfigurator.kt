package com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.linkedIn

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.socialmedia.SocialmediaUtils
import com.jetbrains.edu.learning.socialmedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.HyperskillSocialmediaUtils
import java.nio.file.Path
import kotlin.random.Random

class HyperskillLinkedInConfigurator : LinkedInPluginConfigurator {
  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    return HyperskillSocialmediaUtils.askToPost(project, solvedTask, statusBeforeCheck)
  }

  override fun getDefaultMessage(solvedTask: Task): String {
    val course = solvedTask.course
    val courseName = (course as? HyperskillCourse)?.getProjectLesson()?.presentableName ?: course.presentableName
    return EduCoreBundle.message("hyperskill.linkedin.message", courseName)
  }

  override fun getImagePath(solvedTask: Task): Path? {
    val gifIndex = Random.Default.nextInt(NUMBER_OF_IMAGES)
    return SocialmediaUtils.pluginRelativePath("socialmedia/linkedin/hyperskill/achievement$gifIndex.gif")
  }

  companion object {
    private const val NUMBER_OF_IMAGES = 2
  }
}
