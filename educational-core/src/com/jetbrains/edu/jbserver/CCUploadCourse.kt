package com.jetbrains.edu.jbserver

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse


class CCUploadCourse : DumbAwareAction("&Upload Course to EduServer", "Upload Course to EduServer", null) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val eduCourse = if (course is EduCourse) course else EduCourse(course)
    CourseSynchronizer(course, project)
    if (eduCourse.courseId == 0) {
      // First upload
      ServerConnector.createCourse(eduCourse)
      StudyTaskManager.getInstance(project).course = eduCourse
      EduUtils.notify("Course upload", "Course `${eduCourse.name}` uploaded successfully!", NotificationType.INFORMATION)
    } else {
      // Update upload
      ServerConnector.updateCourse(eduCourse)
      EduUtils.notify("Course upload", "Course `${eduCourse.name}` updated successfully", NotificationType.INFORMATION)
    }
  }

}
