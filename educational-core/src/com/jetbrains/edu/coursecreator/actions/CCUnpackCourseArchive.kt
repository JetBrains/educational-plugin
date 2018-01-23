package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduUtils

class CCUnpackCourseArchive : CCNewCourseActionBase("Unpack Course From Archive", "Unpack Course From Archive") {
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

    val dialog = CCNewCourseDialog("Unpack Course", "Unpack", course, this::initializeCourseProject)
    dialog.show()
  }
}
