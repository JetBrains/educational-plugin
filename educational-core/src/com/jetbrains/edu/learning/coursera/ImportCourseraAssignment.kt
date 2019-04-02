package com.jetbrains.edu.learning.coursera

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.courseFormat.Course

class ImportCourseraAssignment : ImportLocalCourseAction("Start Coursera Assignment") {

  override fun initCourse(course: Course): Course {
    super.initCourse(course)
    return courseraCourseFromLocal(course)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.icon = AllIcons.ToolbarDecorator.Import
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
  }
}