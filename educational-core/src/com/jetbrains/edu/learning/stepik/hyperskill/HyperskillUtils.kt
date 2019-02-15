package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

val HYPERSKILL_STAGE: Key<Int> = Key.create("HYPERSKILL_STAGE")

fun openSelectedStage(course: Course, project: Project) {
  val stageId = course.getUserData(HYPERSKILL_STAGE) ?: return
  if (course is HyperskillCourse && stageId > 0) {
    val index = course.stages.indexOfFirst { stage -> stage.id == stageId }
    if (course.lessons.isNotEmpty()) {
      val taskList = course.lessons[0].taskList
      if (taskList.size > index) {
        NavigationUtils.navigateToTask(project, taskList[index], taskList[0], false)
      }
    }
  }
}
