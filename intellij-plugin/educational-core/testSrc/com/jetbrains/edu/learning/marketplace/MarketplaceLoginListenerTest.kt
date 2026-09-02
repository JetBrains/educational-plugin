package com.jetbrains.edu.learning.marketplace

import com.intellij.util.application
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceLoginListener
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.SubmissionsTestBase
import io.mockk.*
import org.junit.Test

class MarketplaceLoginListenerTest : SubmissionsTestBase() {

  @Test
  fun `test login prepares submissions content and uploads local submissions`() {
    val submissionsManager = mockService<SubmissionsManager>(project)
    every { submissionsManager.prepareSubmissionsContentWhenLoggedIn(any()) } answers {
      firstArg<() -> Unit>().invoke()
    }
    val connector = mockService<MarketplaceSubmissionsConnector>(application)
    val loader = mockService<MarketplaceSolutionLoader>(project)
    coJustRun { connector.uploadLocalSubmissions(any(), any()) }
    justRun { loader.loadSolutionsInBackground() }
    val course = createEduCourse()

    application.messageBus.syncPublisher(MarketplaceLoginListener.LOGIN_TOPIC).onLoginSuccess()

    verify(exactly = 1) { submissionsManager.prepareSubmissionsContentWhenLoggedIn(any()) }
    coVerify(ordering = Ordering.ORDERED, timeout = 1000) {
      connector.uploadLocalSubmissions(project, course)
      loader.loadSolutionsInBackground()
    }
  }

  private fun createEduCourse(): EduCourse = courseWithFiles(
    language = FakeGradleBasedLanguage,
    courseProducer = ::EduCourse,
    id = 1
  ) {
    lesson("lesson1") {
      eduTask("task1", stepId = 1) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("test/Tests1.kt", "fun tests1() {}")
      }
      eduTask("task2", stepId = 2) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("test/Tests2.kt", "fun tests2() {}")
      }
    }
  }.apply {
    isMarketplace = true
    marketplaceCourseVersion = 1
  } as EduCourse
}
