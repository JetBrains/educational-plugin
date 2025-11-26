package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCEditCourseArchive : DumbAwareAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val descriptor = FileChooserDescriptorFactory
      .singleFile()
      .withExtensionFilter("zip")
    val virtualFile = FileChooser.chooseFile(descriptor, project, null) ?: return
    val course = EduUtilsKt.getLocalCourse(virtualFile.path)
    if (course == null) {
      Messages.showErrorDialog(
        EduCoreBundle.message("dialog.message.course.incompatible"),
        EduCoreBundle.message("dialog.title.failed.to.unpack.course")
      )
      return
    }

    CCNewCourseDialog(EduCoreBundle.message("dialog.title.unpack.course"), EduCoreBundle.message("button.unpack"), course).show()
  }
}
