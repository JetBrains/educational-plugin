package com.jetbrains.edu.socialMedia.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.messages.BUNDLE
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import org.jetbrains.annotations.PropertyKey

object HyperskillSocialMediaUtils {

  fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
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

  fun getMessage(solvedTask: Task, @PropertyKey(resourceBundle = BUNDLE) messageKey: String): String {
    val course = solvedTask.course
    val courseName = (course as? HyperskillCourse)?.getProjectLesson()?.presentableName ?: course.presentableName
    return EduSocialMediaBundle.message(messageKey, courseName)
  }

  // NB! This number should be synchronized for X and LinkedIn to post the same images to both social networks
  const val NUMBER_OF_GIFS = 2
}
