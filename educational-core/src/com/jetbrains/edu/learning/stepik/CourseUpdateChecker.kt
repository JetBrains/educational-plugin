package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.EduUtils.isNewlyCreated
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.atomic.AtomicInteger
import com.intellij.openapi.diagnostic.Logger

abstract class CourseUpdateChecker<T : Course>(protected val project: Project,
                                               protected val course: T,
                                               protected val disposable: Disposable) {

  private val checkRunnable = Runnable { (checkIsUpToDate()).doWhenDone { queueNextCheck(getCheckInterval()) } }
  private val checkForAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, disposable)

  private val invocationCounter: AtomicInteger = AtomicInteger()

  fun check() {
    if (!course.canBeUpdated()) {
      return
    }
    if (isNewlyCreated(project)) {
      queueNextCheck(getCheckInterval())
    }
    else {
      checkRunnable.run()
    }
  }

  protected abstract fun Course.canBeUpdated(): Boolean

  private fun queueNextCheck(interval: Long) {
    LOG.info("Scheduled next is course up to date check for ${course.name} with check interval $interval seconds")
    checkForAlarm.addRequest(checkRunnable, interval)
  }

  private fun checkIsUpToDate(): ActionCallback {
    val actionCallback = ActionCallback()
    doCheckIsUpToDate {
      if (isUnitTestMode) {
        invocationCounter.incrementAndGet()
      }
      actionCallback.setDone()
    }
    return actionCallback
  }

  protected abstract fun doCheckIsUpToDate(onFinish: () -> Unit)

  //default value is 18 000 seconds (5 hours), set in educational-core.xml
  private fun getCheckInterval(): Long = DateFormatUtil.SECOND * Registry.intValue(REGISTRY_KEY)

  @TestOnly
  fun invocationNumber(): Int = invocationCounter.get()

  companion object {
    const val REGISTRY_KEY = "edu.course.update.check.interval"
    private val LOG = Logger.getInstance(CourseUpdateChecker::class.java)
  }
}