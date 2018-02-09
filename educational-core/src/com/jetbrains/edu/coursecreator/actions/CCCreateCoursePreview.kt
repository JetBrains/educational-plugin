package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.configurator

class CCCreateCoursePreview : DumbAwareAction("&Create Course Preview") {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val projectDir = project.baseDir ?: return
    val module = ModuleUtil.findModuleForFile(projectDir, project) ?: return
    val currentCourse = StudyTaskManager.getInstance(project).course ?: return
    val configurator = currentCourse.configurator ?: return

    CCCreateCoursePreviewDialog(project, module, currentCourse, configurator).show()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && CCUtils.isCourseCreator(project)
  }
}
