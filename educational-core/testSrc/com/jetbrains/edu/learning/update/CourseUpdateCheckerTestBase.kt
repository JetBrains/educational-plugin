package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils

abstract class CourseUpdateCheckerTestBase : EduTestCase() {

  protected fun doTest(updateChecker: CourseUpdateChecker,
                       isCourseUpToDate: Boolean,
                       invocationNumber: Int,
                       afterTimeoutInvocationNumber: Int,
                       checkInterval: Int = 1,
                       isCourseUpToDateCheck: () -> Unit
  ) {
    val notificationListener = NotificationListener(project, testRootDisposable)
    withCustomCheckInterval(checkInterval) {
      updateChecker.check()
      assertEquals(invocationNumber, updateChecker.invocationNumber)
      testCheckScheduled(afterTimeoutInvocationNumber, updateChecker)
      checkNotification(notificationListener, isCourseUpToDate)
      isCourseUpToDateCheck()
    }
  }

  protected abstract fun checkNotification(notificationListener: NotificationListener, isCourseUpToDate: Boolean)

  protected fun testNoCheck(updateChecker: CourseUpdateChecker) {
    withCustomCheckInterval(2) {
      updateChecker.check()
      val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(1000) }
      EduUtils.waitAndDispatchInvocationEvents(future)
      assertEquals(0, updateChecker.invocationNumber)
    }
  }

  private fun withCustomCheckInterval(interval: Int, action: () -> Unit) {
    val registryValue = Registry.get(CourseUpdateChecker.REGISTRY_KEY)
    val oldValue = registryValue.asInteger()
    registryValue.setValue(interval)
    try {
      action()
    }
    finally {
      registryValue.setValue(oldValue)
    }
  }

  private fun testCheckScheduled(expectedInvocationNumber: Int, updateChecker: CourseUpdateChecker) {
    val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(3000) }
    EduUtils.waitAndDispatchInvocationEvents(future)
    check(expectedInvocationNumber <= updateChecker.invocationNumber)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/"

  @Suppress("DEPRECATION")
  class NotificationListener(project: Project, disposable: Disposable) {
    var notificationShown = false
    var notificationText = ""
    private val connection = project.messageBus.connect(disposable)

    init {
      connection.subscribe(Notifications.TOPIC, object : Notifications {
        override fun notify(notification: Notification) {
          notificationShown = true
          notificationText = notification.content
        }
      })
    }
  }
}