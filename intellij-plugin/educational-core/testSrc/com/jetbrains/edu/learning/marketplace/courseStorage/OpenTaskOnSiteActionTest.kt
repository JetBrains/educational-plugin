package com.jetbrains.edu.learning.marketplace.courseStorage

import com.intellij.util.application
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
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
    CourseStorageLinkSettings.getInstance(project).link = link
    CourseStorageLinkSettings.getInstance(project).platformName = "AWS"

    // some task needs to be opened for the action updated to be executed
    val task = findTask(0, 0)
    val taskFile = task.getTaskFile("Task.kt")!!
    val virtualFile = taskFile.getVirtualFile(project)!!
    myFixture.openFileInEditor(virtualFile)

    val mockedBrowserUtil = mockService<EduBrowser>(application)
    every { mockedBrowserUtil.browse(link) } returns Unit

    testAction(OpenTaskOnSiteAction.ACTION_ID, shouldBeEnabled = true, shouldBeVisible = true)

    verify(exactly = 1) { mockedBrowserUtil.browse(link) }
  }

  @Test
  fun `test open on site action is disabled if link is absent`() {
    CourseStorageLinkSettings.getInstance(project).platformName = "AWS"

    val task = findTask(0, 0)
    val taskFile = task.getTaskFile("Task.kt")!!
    val virtualFile = taskFile.getVirtualFile(project)!!
    myFixture.openFileInEditor(virtualFile)

    testAction(OpenTaskOnSiteAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = false)
  }
}