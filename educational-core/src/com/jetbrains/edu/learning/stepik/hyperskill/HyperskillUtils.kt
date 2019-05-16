package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.lang.Language
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

val HYPERSKILL_STAGE: Key<Int> = Key.create("HYPERSKILL_STAGE")
const val HYPERSKILL_GROUP_ID = "Hyperskill.post"

fun openSelectedStage(course: Course, project: Project) {
  val stageId = course.getUserData(HYPERSKILL_STAGE) ?: return
  if (course is HyperskillCourse && stageId > 0) {
    val index = course.stages.indexOfFirst { stage -> stage.id == stageId }
    if (course.lessons.isNotEmpty()) {
      val lesson = course.lessons[0]
      val taskList = lesson.taskList
      if (taskList.size > index) {
        val fromTask = if (lesson is FrameworkLesson) lesson.currentTask() else taskList[0]
        NavigationUtils.navigateToTask(project, taskList[index], fromTask, false)
      }
    }
  }
}

fun isHyperskillSupportAvailable(): Boolean {
  return Language.findLanguageByID(EduNames.JAVA) != null && !EduUtils.isAndroidStudio()
}

fun showFailedToPostNotification() {
  val notification = Notification(HYPERSKILL_GROUP_ID, "Failed to post submission to the Hyperskill",
                                  "Please, try to check again", NotificationType.WARNING)
  notification.notify(null)
}