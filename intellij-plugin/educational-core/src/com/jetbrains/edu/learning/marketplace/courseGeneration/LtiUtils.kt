package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.navigation.NavigationUtils

fun openStudyItem(studyItemId: Int, project: Project) {
  if (studyItemId == -1) return
  val course = project.course ?: return

  course.allTasks.firstOrNull {
    it.id == studyItemId
    || it.lesson.id == studyItemId
    || it.lesson.section?.id == studyItemId
  }?.let {
    NavigationUtils.navigateToTask(project, it)
  }
}