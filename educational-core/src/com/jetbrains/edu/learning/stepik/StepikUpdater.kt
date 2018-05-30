package com.jetbrains.edu.learning.stepik

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.Ref
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.InitializationComponent
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.RemoteCourse

import java.util.ArrayList
import java.util.Date

class StepikUpdater(application: Application) {

  private val myCheckRunnable = Runnable { updateCourseList().doWhenDone { queueNextCheck(CHECK_INTERVAL) } }
  private val myCheckForUpdatesAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)

  init {
    scheduleCourseListUpdate(application)
  }

  private fun scheduleCourseListUpdate(application: Application) {
    if (!checkNeeded()) {
      return
    }
    application.messageBus.connect(application).subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
      override fun appFrameCreated(commandLineArgs: Array<String>?, willOpenProject: Ref<Boolean>) {

        val timeToNextCheck = EduSettings.getInstance().lastTimeChecked + CHECK_INTERVAL - System.currentTimeMillis()
        if (timeToNextCheck <= 0) {
          myCheckRunnable.run()
        }
        else {
          queueNextCheck(timeToNextCheck)
        }
      }
    })
  }

  private fun queueNextCheck(interval: Long) {
    myCheckForUpdatesAlarm.addRequest(myCheckRunnable, interval)
  }

  companion object {
    private val CHECK_INTERVAL = DateFormatUtil.DAY

    private fun updateCourseList(): ActionCallback {
      val callback = ActionCallback()
      ApplicationManager.getApplication().executeOnPooledThread {
        val courses = StepikConnector.getCourses(null)
        EduSettings.getInstance().lastTimeChecked = System.currentTimeMillis()

        if (!courses.isEmpty()) {
          val updated = ArrayList<Course>()
          for (course in courses) {
            if (course is RemoteCourse && course.updateDate.after(
                Date(EduSettings.getInstance().lastTimeChecked))) {
              updated.add(course)
            }
          }
          if (updated.isEmpty()) return@executeOnPooledThread
          val message: String
          val title: String
          if (updated.size == 1) {
            message = updated[0].name
            title = "New course available"
          } else {
            title = "New courses available"
            message = updated.joinToString(separator = ", ") { it.name }
          }
          val notification = Notification("New.course", title, message, NotificationType.INFORMATION)
          notification.notify(null)
        }
      }
      return callback
    }

    private fun checkNeeded(): Boolean {
      if (!PropertiesComponent.getInstance().isValueSet(InitializationComponent.CONFLICTING_PLUGINS_DISABLED)) {
        return false
      }
      val timeToNextCheck = EduSettings.getInstance().lastTimeChecked + CHECK_INTERVAL - System.currentTimeMillis()
      return timeToNextCheck <= 0
    }
  }
}
