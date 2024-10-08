package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.updateEnvironmentSettings

@Suppress("ComponentNotRegistered")  // educational-core.xml
class CCCreateCoursePreview : DumbAwareAction() {

 override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val currentCourse = StudyTaskManager.getInstance(project).course ?: return
    if (currentCourse !is EduCourse) return
    val configurator = currentCourse.configurator ?: return
    // The course preview dialog opens BEFORE the course archive is created, so we need to update environment settings
    // before creating the course archive.
    currentCourse.updateEnvironmentSettings(project, configurator)

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
