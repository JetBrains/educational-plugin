package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.util.application
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.testAction
import io.mockk.every
import io.mockk.verify
import org.junit.Test

class OpenTaskOnSiteActionTest : EduActionTestCase() {

  override fun setUp() {
    super.setUp()
    courseWithFiles(id = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3) {
          taskFile("Task.kt")
        }
        eduTask("task2", stepId = 4)
      }
    } as EduCourse
  }

  @Test
  fun `test open on site action is visible and enabled if link is present`() {
    val link = "https://academy.jetbrains.com"
    OpenOnSiteLinkSettings.getInstance(project).link = link

    // some task needs to be opened for the action updated to be executed
    val virtualFile = findFile("lesson1/task1/Task.kt")
    myFixture.openFileInEditor(virtualFile)

    val mockedBrowserUtil = mockService<EduBrowser>(application)
    every { mockedBrowserUtil.browse(link) } returns Unit

    testAction(OpenTaskOnSiteAction.ACTION_ID, shouldBeEnabled = true, shouldBeVisible = true)

    verify(exactly = 1) { mockedBrowserUtil.browse(link) }
  }

  @Test
  fun `test open on site action is disabled if link is absent`() {

    val virtualFile = findFile("lesson1/task1/Task.kt")
    myFixture.openFileInEditor(virtualFile)

    testAction(OpenTaskOnSiteAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = false)
  }
}