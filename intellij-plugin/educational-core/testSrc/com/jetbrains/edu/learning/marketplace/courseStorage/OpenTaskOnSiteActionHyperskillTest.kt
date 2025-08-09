package com.jetbrains.edu.learning.marketplace.courseStorage

import com.intellij.util.application
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillTaskLink
import com.jetbrains.edu.learning.testAction
import io.mockk.every
import io.mockk.verify
import org.junit.Test

class OpenTaskOnSiteActionHyperskillTest : EduActionTestCase() {
  override fun setUp() {
    super.setUp()
    courseWithFiles(
      id = 1,
      courseProducer = ::HyperskillCourse,
    ) {
      lesson("lesson1", id = 2) {
        eduTask("task1", stepId = 3) {
          taskFile("Task.kt")
        }
        eduTask("task2", stepId = 4)
      }
    }
  }

  @Test
  fun `test on site action is enabled for hyperskill courses`() {
    val task = findTask(0, 0)
    val taskFile = task.getTaskFile("Task.kt")!!
    val virtualFile = taskFile.getVirtualFile(project)!!
    myFixture.openFileInEditor(virtualFile)

    val mockedBrowserUtil = mockService<EduBrowser>(application)
    val link = hyperskillTaskLink(task)
    every { mockedBrowserUtil.browse(link) } returns Unit

    testAction(OpenTaskOnSiteAction.ACTION_ID, shouldBeEnabled = true, shouldBeVisible = true)
    verify(exactly = 1) { mockedBrowserUtil.browse(link) }
  }
}