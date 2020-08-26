package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered")  // educational-core.xml
class CCUnpackCourseArchive : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.unpack.course.archive.text"),
  EduCoreBundle.lazyMessage("action.unpack.course.archive.description"),
  null
) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val descriptor = FileChooserDescriptor(true, true, true, true,
                                           true, false)
    val virtualFile = FileChooser.chooseFile(descriptor, project, null) ?: return
    val course = EduUtils.getLocalCourse(virtualFile.path)
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
