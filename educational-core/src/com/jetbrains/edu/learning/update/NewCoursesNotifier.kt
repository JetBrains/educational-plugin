package com.jetbrains.edu.learning.update

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.Ref
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.EduCoursesProvider
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class NewCoursesNotifier : Disposable {

  private val myCheckRunnable = Runnable { updateCourseList().doWhenDone { queueNextCheck(ourCheckInterval) } }
  private val myCheckForUpdatesAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  fun scheduleNotification() {
    ApplicationManager.getApplication().messageBus.connect().subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
      override fun appFrameCreated(commandLineArgs: Array<String>?, willOpenProject: Ref<Boolean>) {
        scheduleNotificationInternal()
      }
    })
  }

  @VisibleForTesting
  fun scheduleNotificationInternal() {
    val timeToNextCheck = EduSettings.getInstance().lastTimeChecked + ourCheckInterval - System.currentTimeMillis()
    return if (timeToNextCheck <= 0) {
      myCheckRunnable.run()
    } else {
      queueNextCheck(timeToNextCheck)
    }
  }

  private fun queueNextCheck(interval: Long) {
    myCheckForUpdatesAlarm.addRequest(myCheckRunnable, interval)
  }

  private fun updateCourseList(): ActionCallback {
    val callback = ActionCallback()

    ApplicationManager.getApplication().executeOnPooledThread {
      val courses = EduCoursesProvider.loadAllCourses()

      val updated = courses.filterIsInstance<RemoteCourse>()
        .filter { it.updateDate.after(Date(EduSettings.getInstance().lastTimeChecked)) }
      if (!updated.isEmpty()) {
        showNewCoursesNotification(updated)
      }
      EduSettings.getInstance().lastTimeChecked = System.currentTimeMillis()
      if (isUnitTestMode) {
        INVOCATION_COUNTER.incrementAndGet()
      }
      callback.setDone()
    }
    return callback
  }

  override fun dispose() {}

  @TestOnly
  fun setNewCheckInterval(newInterval: Long): Long {
    val oldValue = ourCheckInterval
    ourCheckInterval = newInterval
    return oldValue
  }

  @TestOnly
  fun invocationNumber(): Int = INVOCATION_COUNTER.get()

  companion object {
    private var ourCheckInterval = DateFormatUtil.DAY

    private val INVOCATION_COUNTER: AtomicInteger = AtomicInteger()
  }
}
