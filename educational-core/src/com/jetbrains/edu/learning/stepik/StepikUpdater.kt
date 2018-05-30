package com.jetbrains.edu.learning.stepik

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.Ref
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.InitializationComponent
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.CoursePanel
import java.util.*
import javax.swing.JComponent

class StepikUpdater(application: Application) {

  private val myCheckRunnable = Runnable { updateCourseList().doWhenDone { queueNextCheck(CHECK_INTERVAL) } }
  private val myCheckForUpdatesAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)

  init {
    scheduleCourseListUpdate(application)
  }

  private fun scheduleCourseListUpdate(application: Application) {
    if (!checkNeeded()) return

    application.messageBus.connect(application).subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
      override fun appFrameCreated(commandLineArgs: Array<String>?, willOpenProject: Ref<Boolean>) {
        val timeToNextCheck = EduSettings.getInstance().lastTimeChecked + CHECK_INTERVAL - System.currentTimeMillis()
        if (timeToNextCheck <= 0) {
          myCheckRunnable.run()
        } else {
          queueNextCheck(timeToNextCheck)
        }
      }
    })
  }

  private fun queueNextCheck(interval: Long) {
    myCheckForUpdatesAlarm.addRequest(myCheckRunnable, interval)
  }

  private fun updateCourseList(): ActionCallback {
    val callback = ActionCallback()

    ApplicationManager.getApplication().executeOnPooledThread {
      val courses = StepikConnector.getCourses(null)
      EduSettings.getInstance().lastTimeChecked = System.currentTimeMillis()

      val updated = courses.filterIsInstance<RemoteCourse>()
        .filter { it.updateDate.after(Date(EduSettings.getInstance().lastTimeChecked)) }
      if (!updated.isEmpty()) {
        val title = if (updated.size == 1) "New course available" else "New courses available"
        val message = updated.joinToString(separator = " <br> ") { "<a href=\"${it.id}\">${it.name}</a>" }
        val notification = Notification("New.course", title, message, NotificationType.INFORMATION, NotificationListener { notification, e ->
          notification.expire()
          val course = updated.find { it.id.toString() == e.description } ?: return@NotificationListener
          val configurator = course.configurator ?: return@NotificationListener
          SingleCourseDialog(course, configurator).show()
        })
        notification.notify(null)
      }
      callback.setDone()
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

  companion object {
    private const val CHECK_INTERVAL = DateFormatUtil.DAY
  }

  private class SingleCourseDialog(
    private val myCourse: Course,
    private val myConfigurator: EduConfigurator<*>
  ) : DialogWrapper(true) {

    private val myPanel: CoursePanel = CoursePanel(/*isIndependent = */ true, /*isLocationFieldNeeded = */ true).apply {
      preferredSize = JBUI.size(WIDTH, HEIGHT)
      minimumSize = JBUI.size(WIDTH, HEIGHT)
    }

    init {
      title = "Create Course"
      setOKButtonText("Create")
      myPanel.bindCourse(myCourse)
      init()
    }

    override fun createCenterPanel(): JComponent = myPanel

    override fun doOKAction() {
      val settings = myPanel.projectSettings
      val location = myPanel.locationString ?: error("Location should be not null")
      myConfigurator.courseBuilder
        .getCourseProjectGenerator(myCourse)
        ?.doCreateCourseProject(location, settings)
      close(OK_EXIT_CODE)
    }

    companion object {
      private const val WIDTH: Int = 370
      private const val HEIGHT: Int = 330
    }
  }
}
