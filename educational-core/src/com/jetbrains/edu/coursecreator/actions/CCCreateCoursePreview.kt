package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered")  // educational-core.xml
class CCCreateCoursePreview : DumbAwareAction(EduCoreBundle.lazyMessage("action.create.course.preview.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val currentCourse = StudyTaskManager.getInstance(project).course ?: return
    if (currentCourse !is EduCourse) return
    val configurator = currentCourse.configurator ?: return

    CCCreateCoursePreviewDialog(project, currentCourse, configurator).show()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val course = StudyTaskManager.getInstance(project).course
    e.presentation.isEnabledAndVisible = course is EduCourse && CCUtils.isCourseCreator(project)
  }
}
