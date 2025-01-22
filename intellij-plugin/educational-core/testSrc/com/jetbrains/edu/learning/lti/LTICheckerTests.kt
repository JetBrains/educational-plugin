package com.jetbrains.edu.learning.lti

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.lti.LTIConnector
import com.jetbrains.edu.learning.marketplace.lti.LTIOnlineService
import com.jetbrains.edu.learning.marketplace.lti.LTISettingsManager
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import org.junit.Test

class LTICheckerTests : EduTestCase() {

  private val mockLTIConnector: MockLTIConnector
    get() = LTIConnector.getInstance() as MockLTIConnector

  private val ltiSettings
    get() = LTISettingsManager.instance(project).state

  private var launchId: String
    get() = ltiSettings.launchId!!
    set(value) {
      ltiSettings.launchId = value
    }

  override fun setUp() {
    super.setUp()
    launchId = "some random launch id"
  }

  @Test
  fun `alpha test lti reports nothing on failed task`() {
    ltiSettings.onlineService = LTIOnlineService.ALPHA_TEST_2024
    doTest(solved = false) { urlPath, body ->
      if (urlPath != null) {
        error("LTI should not report anything for a failed check")
      }
    }
  }

  @Test
  fun `alpha test lti reports something on solved task`() {
    ltiSettings.onlineService = LTIOnlineService.ALPHA_TEST_2024
    doTest(solved = true) { urlPath, body ->
      if (urlPath == null) {
        error("LTI should report something for a solved check")
      }
    }
  }

  @Test
  fun `lti report for unsolved task`() {
    ltiSettings.onlineService = LTIOnlineService.STANDALONE
    doTest(solved = false) { _, body ->
      assertEquals("Unexpected request body", "completed=false", body)
    }
  }

  @Test
  fun `lti report for solved task`() {
    ltiSettings.onlineService = LTIOnlineService.STANDALONE
    doTest(solved = true) { _, body ->
      assertEquals("Unexpected request body", "completed=true", body)
    }
  }

  fun doTest(solved: Boolean, action: (urlPath: String?, body: String?) -> Unit) {
    val course = courseWithFiles(id=1) {
      lesson {
        eduTask {
          taskFile("Task.txt")
          taskFile("checkResult.txt", if (solved) {
            "Solved"
          }
          else {
            "Failed"
          })
        }
      }
    }.apply { isMarketplace = true }

    var callPath: String? = null
    var body: String? = null
    mockLTIConnector.withResponseHandler(testRootDisposable) { request, path ->
      callPath = path
      body = request.body.readUtf8()
      MockResponseFactory.fromString("")
    }

    val task = course.allTasks[0]
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))

    // exceptions inside the handler are swallowed, so we need to call actions outside the handler
    action(callPath, body)
  }
}