package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

fun openSelectedStage(course: Course, project: Project) {
  val stageId = PropertiesComponent.getInstance().getInt(HYPERSKILL_STAGE, 0)
  if (stageId > 0) {
    val index = (course as HyperskillCourse).stages.indexOfFirst { stage -> stage.id == stageId }
    if (course.lessons.isNotEmpty()) {
      val taskList = course.lessons[0].taskList
      if (taskList.size > index) {
        NavigationUtils.navigateToTask(project, taskList[index], taskList[0])
      }
    }
  }
}
