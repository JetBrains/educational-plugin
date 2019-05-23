package com.jetbrains.edu.learning.update

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ActionCallback
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.AppLifecycleListenerAdapter
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.featuredCourses
import com.jetbrains.edu.learning.stepik.isSignificantlyAfter
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class NewCoursesNotifier(parentDisposable: Disposable, private val featuredCoursesProvider: () -> List<Int> = { featuredCourses }) {

  private val checkRunnable = Runnable { updateCourseList().doWhenDone { queueNextCheck(checkInterval) } }
  private val checkForNotifyAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable)

  private val invocationCounter: AtomicInteger = AtomicInteger()

  fun scheduleNotification() {
    ApplicationManager.getApplication().messageBus.connect().subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListenerAdapter {
      override fun appFrameCreated() {
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

    val ids = EduSettings.getInstance().shownCourseIds

    ApplicationManager.getApplication().executeOnPooledThread {
      val courses = CoursesProvider.loadRemoteCourses()
      val featuredCourses = featuredCoursesProvider()
      val newlyCreated = courses.filterIsInstance<EduCourse>()
        .filter { it.id !in ids && it.createDate.isSignificantlyAfter(Date(EduSettings.getInstance().lastTimeChecked)) && it.id in featuredCourses }
      if (!newlyCreated.isEmpty()) {
        showNewCoursesNotification(newlyCreated)
        newlyCreated.mapTo(ids) { it.id }
      }

      EduSettings.getInstance().shownCourseIds = ids
      EduSettings.getInstance().lastTimeChecked = System.currentTimeMillis()
      if (isUnitTestMode) {
        invocationCounter.incrementAndGet()
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
  fun invocationNumber(): Int = invocationCounter.get()

  companion object {
    private var checkInterval = DateFormatUtil.DAY
  }
}
