package com.jetbrains.edu.learning.update

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.Ref
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.isSignificantlyAfter
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class NewCoursesNotifier(parentDisposable: Disposable) {

  private val checkRunnable = Runnable { updateCourseList().doWhenDone { queueNextCheck(checkInterval) } }
  private val checkForNotifyAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable)

  fun scheduleNotification() {
    ApplicationManager.getApplication().messageBus.connect().subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
      override fun appFrameCreated(commandLineArgs: Array<String>?, willOpenProject: Ref<Boolean>) {
        scheduleNotificationInternal()
      }
    })
  }

  @VisibleForTesting
  fun scheduleNotificationInternal() {
    val timeBeforeNextCheck = EduSettings.getInstance().lastTimeChecked + checkInterval - System.currentTimeMillis()
    return if (timeBeforeNextCheck <= 0) {
      checkRunnable.run()
    } else {
      queueNextCheck(timeBeforeNextCheck)
    }
  }

  private fun queueNextCheck(interval: Long) {
    checkForNotifyAlarm.addRequest(checkRunnable, interval)
  }

  private fun updateCourseList(): ActionCallback {
    val callback = ActionCallback()

    ApplicationManager.getApplication().executeOnPooledThread {
      val courses = CoursesProvider.loadAllCourses()

      val updated = courses.filterIsInstance<RemoteCourse>()
        .filter {it.updateDate.isSignificantlyAfter(Date(EduSettings.getInstance().lastTimeChecked)) }
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

  @TestOnly
  fun setNewCheckInterval(newInterval: Long): Long {
    val oldValue = checkInterval
    checkInterval = newInterval
    return oldValue
  }

  @TestOnly
  fun invocationNumber(): Int = INVOCATION_COUNTER.get()

  companion object {
    private var checkInterval = DateFormatUtil.DAY

    private val INVOCATION_COUNTER: AtomicInteger = AtomicInteger()
  }
}
