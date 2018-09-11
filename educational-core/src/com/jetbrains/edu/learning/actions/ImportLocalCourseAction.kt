package com.jetbrains.edu.learning.coursera

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.LocalCourseFileChooser
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.statistics.EduUsagesCollector

open class ImportLocalCourseAction(text: String = "Import Local Course") : DumbAwareAction(text) {
  override fun actionPerformed(e: AnActionEvent?) {
    FileChooser.chooseFile(LocalCourseFileChooser, null, VfsUtil.getUserHomeDir()) { file ->
      val fileName = file.path
      val course = EduUtils.getLocalCourse(fileName)
      if (course != null) {
        initCourse(course)
        EduUsagesCollector.courseArchiveImported()
        JoinCourseDialog(course).show()
      }
      else {
        Messages.showErrorDialog("Selected archive doesn't contain a valid course", "Failed to Add Local Course")
      }
    }
  }

  protected open fun initCourse(course: Course) {
    course.isFromZip = true
  }
}