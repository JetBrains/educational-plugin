package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduUtils

class CCUnpackCourseArchive : DumbAwareAction("Unpack Course From Archive", "Unpack Course From Archive", null) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val descriptor = FileChooserDescriptor(true, true, true, true,
                                           true, false)
    val virtualFile = FileChooser.chooseFile(descriptor, project, null) ?: return
    val course = EduUtils.getLocalCourse(virtualFile.path)
    if (course == null) {
      Messages.showErrorDialog("This course is incompatible with current version", "Failed to Unpack Course")
      return
    }

    CCNewCourseDialog("Unpack Course", "Unpack", course).show()
  }
}
