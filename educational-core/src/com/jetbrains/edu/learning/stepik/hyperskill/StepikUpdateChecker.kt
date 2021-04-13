package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.checkIsStepikUpToDate
import com.jetbrains.edu.learning.stepik.isSignificantlyAfter
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.stepik.updateCourse
import com.jetbrains.edu.learning.update.CourseUpdateChecker

@Service
class StepikUpdateChecker(project: Project) : CourseUpdateChecker(project) {

  override fun courseCanBeUpdated(): Boolean {
    val eduCourse = course as? EduCourse ?: return false
    return eduCourse.isStepikRemote || eduCourse.isStudy  && !eduCourse.isMarketplace
  }

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val eduCourse = course as? EduCourse ?: return

    ApplicationManager.getApplication().executeOnPooledThread {
      val (courseFromStepik, isUpToDate) = eduCourse.checkIsStepikUpToDate()
      runInEdt {
        if (project.isDisposed) return@runInEdt
        eduCourse.isUpToDate = isUpToDate
        if (!isUpToDate) {
          showUpdateAvailableNotification(project) {
            runInBackground(title = message("progress.loading.course")) {
              updateCourse(project, eduCourse)
            }
          }
          eduCourse.markTasksUpToDate(courseFromStepik)
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

  companion object {
    @JvmStatic
    fun getInstance(project: Project): StepikUpdateChecker {
      return project.service()
    }
  }
}