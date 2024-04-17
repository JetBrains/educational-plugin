package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.ide.CopyPasteManager
import com.jetbrains.edu.learning.codeforces.CodeforcesTestCase
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class CodeforcesCopyAndSubmitTest : CodeforcesTestCase() {
  @Test
  fun `test codeforces task contents is copied to clipboard`() {
    val taskFileName = "Task.kt"
    val contents = "123\n456"
    courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask {
          taskFile(taskFileName, contents)
        }
      }
    }
    configureByTaskFile(1, 1, taskFileName)

    testAction(CodeforcesCopyAndSubmitAction.ACTION_ID)
    val actualContents = CopyPasteManager.getInstance().contents!!
    val actualContentsString = actualContents.getTransferData(actualContents.transferDataFlavors.first())
    assertEquals(contents, actualContentsString)
  }

  @Test
  fun `test codeforces Task file is selected for submitting`() {
    val testFileName = "testData/1/input.txt"
    val taskFileName = "Task.txt"
    val contents = "123\n456"
    courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask {
          taskFile("Foo.txt", visible = false)
          taskFile(testFileName)
          taskFile(taskFileName, contents)
        }
      }
    }
    configureByTaskFile(1, 1, testFileName)

    testAction(CodeforcesCopyAndSubmitAction.ACTION_ID)
    val actualContents = CopyPasteManager.getInstance().contents!!
    val actualContentsString = actualContents.getTransferData(actualContents.transferDataFlavors.first())
    assertEquals(contents, actualContentsString)
  }
}
