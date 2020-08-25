package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ide.CopyPasteManager
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse

class CodeforcesCopyAndSubmitTest : CodeforcesTestCase() {
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

    myFixture.testAction(CodeforcesCopyAndSubmitAction())
    val actualContents = CopyPasteManager.getInstance().contents!!
    val actualContentsString = actualContents.getTransferData(actualContents.transferDataFlavors.first())
    assertEquals(contents, actualContentsString)
  }
}
