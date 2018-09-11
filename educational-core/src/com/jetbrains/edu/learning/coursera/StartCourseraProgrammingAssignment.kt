package com.jetbrains.edu.learning.coursera

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.newproject.LocalCourseFileChooser
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog

class StartCourseraProgrammingAssignment : DumbAwareAction("Start Coursera Programming Assignment") {
  override fun actionPerformed(e: AnActionEvent?) {
    FileChooser.chooseFile(LocalCourseFileChooser, null, VfsUtil.getUserHomeDir()) { file ->
      val fileName = file.path
      val course = EduUtils.getLocalCourse(fileName)
      if (course != null) {
        course.isFromZip = true
        course.courseType = CourseraNames.COURSE_TYPE
        EduUsagesCollector.courseArchiveImported()
        JoinCourseDialog(course).show()
      }
      else {
        Messages.showErrorDialog("Selected archive doesn't contain a valid course", "Failed to Add Local Course")
      }
    }
  }

  companion object {
    const val ACTION_ID = "Educational.StartCourseraAssignment"
  }
}