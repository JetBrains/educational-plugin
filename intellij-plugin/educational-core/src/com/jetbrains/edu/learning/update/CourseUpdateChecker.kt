package com.jetbrains.edu.learning.update

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Alarm
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.EduUtilsKt.isNewlyCreated
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class CourseUpdateChecker(protected val project: Project) : Disposable, EduTestAware {
  private val checkRunnable = Runnable { (checkIsUpToDate()).doWhenDone { queueNextCheck() } }
  private val checkForAlarm by lazy { Alarm(Alarm.ThreadToUse.POOLED_THREAD, this) }
  var course: Course? = project.course
    @TestOnly set
  private val invocationCounter: AtomicInteger = AtomicInteger()
  var invocationNumber: Int
    @TestOnly
    get() = invocationCounter.get()
    @TestOnly
    set(value) = invocationCounter.set(value)

  open val checkInterval: Long
    get() {
      //default value is 18 000 seconds (5 hours), set in educational-core.xml
      return TimeUnit.SECONDS.toMillis(Registry.intValue(REGISTRY_KEY).toLong())
    }

  fun check() {
    if (!courseCanBeUpdated()) {
      return
    }
    if (project.isNewlyCreated()) {
      queueNextCheck()
    }
    else {
      checkRunnable.run()
    }
  }

  @VisibleForTesting
  fun cancelCheckRequests() {
    LOG.info("Is course up to date check requests for ${course?.name} canceled, " +
             "queue contained ${checkForAlarm.activeRequestCount} request(s)")
    checkForAlarm.cancelAllRequests()
  }

  fun queueNextCheck() {
    cancelCheckRequests()
    LOG.info("Scheduled next is course up to date check for ${course?.name} with check interval $checkInterval milliseconds")
    checkForAlarm.addRequest(checkRunnable, checkInterval)
  }

  private fun checkIsUpToDate(): ActionCallback {
    val actionCallback = ActionCallback()
    doCheckIsUpToDate {
      ApplicationManager.getApplication().assertIsDispatchThread()
      if (isUnitTestMode) {
        invocationCounter.incrementAndGet()
      }
      actionCallback.setDone()
    }
    return actionCallback
  }

  /**
   * [onFinish] callback is supposed to be called from EDT
   */
  protected abstract fun doCheckIsUpToDate(onFinish: () -> Unit)

  protected abstract fun courseCanBeUpdated(): Boolean

  override fun dispose() {}

  @TestOnly
  override fun cleanUpState() {
    course = null
  }

  companion object {
    const val REGISTRY_KEY: String = "edu.course.update.check.interval"

    private val LOG: Logger = Logger.getInstance(CourseUpdateChecker::class.java)
  }
}