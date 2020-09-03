package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.getTaskFile

class CodeforcesNavigationTest : NavigationTestBase() {
  fun `test open next task`() {
    configureByTaskFile(1, 1, "src/Foo.kt")
    doTest("src/Bar.kt")
  }

  fun `test open next task non-test file`() {
    configureByTaskFile(1, 2, "src/Bar.kt")
    doTest("src/Baz.kt")
  }

  fun `test unable to open next task file`() {
    configureByTaskFile(1, 3, "src/Baz.kt")
    myFixture.testAction(NextTaskAction())
    assertNull(FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile)
  }

  private fun doTest(expectedFileName: String) {
    myFixture.testAction(NextTaskAction())
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile ?: error("Can't find current file")
    val taskFile = currentFile.getTaskFile(myFixture.project) ?: error("Can't find current task file")
    assertEquals(expectedFileName, taskFile.name)
  }

  override fun createCourse() {
    courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask {
          taskFile("src/Foo.kt")
          taskFile("testData/1/input.txt")
          taskFile("testData/1/output.txt")
        }
        codeforcesTask {
          taskFile("src/Bar.kt")
          taskFile("testData/1/input.txt")
          taskFile("testData/1/output.txt")
        }
        codeforcesTask {
          taskFile("testData/1/input.txt")
          taskFile("testData/1/output.txt")
          taskFile("src/Baz.kt")
        }
        codeforcesTask {
          taskFile("src/Foo.kt", visible = false)
          taskFile("testData/1/input.txt")
          taskFile("testData/1/output.txt")
        }
      }
    }
  }
}
