package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import okhttp3.mockwebserver.MockResponse
import org.apache.http.HttpStatus
import java.io.File
import java.io.IOException

abstract class CourseUpdateCheckerTestBase : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  protected fun doTest(
    updateChecker: CourseUpdateChecker,
    isCourseUpToDate: Boolean,
    invocationNumber: Int,
    afterTimeoutInvocationNumber: Int,
    checkInterval: Int = 2,
    isCourseUpToDateCheck: () -> Unit
  ) {
    val notificationListener = NotificationListener(project, testRootDisposable)
    withCustomCheckInterval(checkInterval) {
      updateChecker.check()
      assertEquals(invocationNumber, updateChecker.invocationNumber)
      updateChecker.checkScheduled(afterTimeoutInvocationNumber, checkInterval)
      checkNotification(notificationListener, isCourseUpToDate)
      isCourseUpToDateCheck()
    }
  }

  open fun checkNotification(notificationListener: NotificationListener,
                             isCourseUpToDate: Boolean,
                             notificationText: String = EduCoreBundle.message("update.content.request")) {
    assertEquals(notificationListener.notificationShown, !isCourseUpToDate)
    if (!isCourseUpToDate) {
      assertEquals(notificationText, notificationListener.notificationText)
    }
  }

  protected fun testNoCheck(updateChecker: CourseUpdateChecker) {
    withCustomCheckInterval(2) {
      updateChecker.check()
      val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(1000) }
      EduActionUtils.waitAndDispatchInvocationEvents(future)
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

  private fun CourseUpdateChecker.checkScheduled(expectedInvocationNumber: Int, checkIntervalInSec: Int) {
    val totalWaitTime = (expectedInvocationNumber + 1) * checkIntervalInSec * 1000
    for (i in 0 until totalWaitTime / 50) {
      Thread.sleep(50)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      if (expectedInvocationNumber <= invocationNumber) return
    }

    error("Update checker invocations should be at least $expectedInvocationNumber but it's only $invocationNumber")
  }

  open fun getTestDataPath(): String = "testData/"

  protected fun mockResponse(fileName: String, responseCode: Int = HttpStatus.SC_OK): MockResponse =
    MockResponseFactory.fromFile(getTestFile(fileName), responseCode)

  @Throws(IOException::class)
  protected fun loadText(fileName: String): String = FileUtil.loadFile(File(getTestDataPath(), fileName))

  protected fun getTestFile(fileName: String) = getTestDataPath() + fileName

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