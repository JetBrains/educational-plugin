package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.stepik.*
import com.jetbrains.edu.learning.stepik.isSignificantlyAfter

class EduCourseUpdateChecker @JvmOverloads constructor(
  project: Project,
  course: EduCourse,
  disposable: Disposable = project
) : CourseUpdateChecker<EduCourse>(project, course, disposable) {

  override fun Course.canBeUpdated(): Boolean {
    return (course as EduCourse).isRemote || course.isStudy
  }

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {

    ApplicationManager.getApplication().executeOnPooledThread {
      val (courseFromStepik, isUpToDate) = course.checkIsUpToDate()
      runInEdt {
        course.isUpToDate = isUpToDate
        if (!isUpToDate) {
          showUpdateAvailableNotification(project) {
            updateCourse(project, course)
          }
          course.markTasksUpToDate(courseFromStepik)
          EditorNotifications.getInstance(project).updateAllNotifications()
        }
        onFinish()
      }
    }
  }

  private fun EduCourse.markTasksUpToDate(courseFromStepik: EduCourse?) {
    if (courseFromStepik == null) return

    val tasksFromServer = courseFromStepik.allTasks.associateBy { it.id }
    visitTasks {
      val taskFromServer = tasksFromServer[it.id]
      if (taskFromServer == null || taskFromServer.updateDate.isSignificantlyAfter(updateDate)) {
        it.isUpToDate = false
      }
    }
  }
}