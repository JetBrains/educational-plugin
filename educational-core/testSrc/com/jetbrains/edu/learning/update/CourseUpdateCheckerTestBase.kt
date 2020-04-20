package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.CourseUpdateChecker
import okhttp3.mockwebserver.MockResponse

abstract class CourseUpdateCheckerTestBase : EduTestCase() {

  protected fun doTest(updateChecker: CourseUpdateChecker<out Course>,
                       isCourseUpToDate: Boolean,
                       invocationNumber: Int,
                       afterTimeoutInvocationNumber: Int,
                       isCourseUpToDateCheck: () -> Unit
  ) {
    val notificationListener = NotificationListener(project, testRootDisposable)
    withCustomCheckInterval(1) {
      updateChecker.check()
      assertEquals(invocationNumber, updateChecker.invocationNumber())
      testCheckScheduled(afterTimeoutInvocationNumber, updateChecker)
      if (isCourseUpToDate) {
        assertFalse("Notification was shown", notificationListener.notificationShown)
      }
      else {
        assertTrue("Notification wasn't shown", notificationListener.notificationShown)
        assertEquals(EduCoreBundle.message("update.content.request"), notificationListener.notificationText)
      }
      assertEquals(notificationListener.notificationShown, !isCourseUpToDate)
      isCourseUpToDateCheck()
    }
  }

  protected fun testNoCheck(updateChecker: CourseUpdateChecker<out Course>) {
    withCustomCheckInterval(2) {
      updateChecker.check()
      val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(1000) }
      EduUtils.waitAndDispatchInvocationEvents(future)
      assertEquals(0, updateChecker.invocationNumber())
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

  private fun testCheckScheduled(expectedInvocationNumber: Int, updateChecker: CourseUpdateChecker<out Course>) {
    val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(3000) }
    EduUtils.waitAndDispatchInvocationEvents(future)
    check(expectedInvocationNumber <= updateChecker.invocationNumber())
  }

  protected fun mockResponse(fileName: String): MockResponse = MockResponseFactory.fromFile(getTestFile(fileName))

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/"

  protected fun getTestFile(fileName: String) = testDataPath + fileName

  @Suppress("DEPRECATION")
  class NotificationListener(project: Project, disposable: Disposable) {
    var notificationShown = false
    var notificationText = ""
    private val connection = project.messageBus.connect(disposable)

    init {
      // BACKCOMPAT: 2019.2
      connection.subscribe(Notifications.TOPIC, object : NotificationsAdapter() {
        override fun notify(notification: Notification) {
          notificationShown = true
          notificationText = notification.content
        }
      })
    }
  }

}