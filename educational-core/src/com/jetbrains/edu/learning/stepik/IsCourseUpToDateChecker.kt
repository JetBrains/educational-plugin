package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.EditorNotifications
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.atomic.AtomicInteger

class IsCourseUpToDateChecker(private val course: EduCourse, private val project: Project) {

  private val checkRunnable = Runnable { (checkIsUpToDate()).doWhenDone { queueNextCheck(getCheckInterval()) } }
  private val checkForAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)

  private val invocationCounter: AtomicInteger = AtomicInteger()

  fun check() {
    if (!course.isRemote || !course.isStudy) {
      return
    }
    checkRunnable.run()
  }

  private fun queueNextCheck(interval: Long) {
    checkForAlarm.addRequest(checkRunnable, interval)
  }

  private fun checkIsUpToDate(): ActionCallback {
    val callback = ActionCallback()

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
        if (isUnitTestMode) {
          invocationCounter.incrementAndGet()
        }
        callback.setDone()
      }
    }
    return callback
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

  //default value is 18 000 seconds (5 hours), set in educational-core.xml
  private fun getCheckInterval(): Long = DateFormatUtil.SECOND * Registry.intValue(REGISTRY_KEY)

  @TestOnly
  fun invocationNumber(): Int = invocationCounter.get()

  companion object {
    const val REGISTRY_KEY = "edu.course.update.check.interval"
  }
}