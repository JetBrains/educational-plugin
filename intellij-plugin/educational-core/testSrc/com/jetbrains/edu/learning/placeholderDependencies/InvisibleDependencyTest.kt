package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduFileEditorTestFixture
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.findTask

class InvisibleDependencyTest : EduTestCase() {

  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    // https://youtrack.jetbrains.com/issue/EDU-6002/Fix-InvisibleDependencyTest-in-232
    if (ApplicationInfo.getInstance().build < BUILD_232) {
      super.runTestRunnable(testRunnable)
    }
  }

  private lateinit var fileEditorFixture: EduFileEditorTestFixture

  override fun setUp() {
    super.setUp()
    fileEditorFixture = EduFileEditorTestFixture(myFixture).apply { setUp() }
  }

  override fun tearDown() {
    fileEditorFixture.tearDown()
    super.tearDown()
  }

  fun `test invisible placeholder with invisible dependency`() = doTest(CheckStatus.Solved, false, "type Bar")
  fun `test visible placeholder with invisible dependency`() = doTest(CheckStatus.Unchecked, true, "type Foo")

  private fun doTest(status: CheckStatus, expectedVisibility: Boolean, expectedSelectedText: String) {
    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", """
          fun foo(): String = <p>TODO()</p>
        """) {
            placeholder(0, "\"Foo\"")
          }
        }
        eduTask("task2") {
          taskFile("Task.kt", """
          fun foo2(): String = <p>type Foo</p>
          fun bar(): String = <p>type Bar</p>
        """) {
            placeholder(0, "\"Foo\"", dependency = "lesson1#task1#Task.kt#1", isVisible = false)
          }
        }
      }
    }

    val task1 = course.findTask("lesson1", "task1")
    task1.openTaskFileInEditor("Task.kt", 0)
    myFixture.type("\"Foo\"")
    task1.status = status

    val task2 = course.findTask("lesson1", "task2")
    task2.openTaskFileInEditor("Task.kt")

    val taskFile = task2.getTaskFile("Task.kt") ?: error("")
    val placeholder = taskFile.answerPlaceholders[0]
    assertEquals(expectedVisibility, placeholder.isVisible)
    assertEquals(expectedSelectedText, myFixture.editor.selectionModel.selectedText)
  }

  companion object {
    private val BUILD_232: BuildNumber = BuildNumber.fromString("232")!!
  }
}
