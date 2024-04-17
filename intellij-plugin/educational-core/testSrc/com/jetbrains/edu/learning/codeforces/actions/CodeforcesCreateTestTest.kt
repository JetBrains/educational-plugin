package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.Consumer
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.Test
import java.awt.event.InputEvent
import java.awt.event.KeyEvent


class CodeforcesCreateTestTest : EduActionTestCase() {

  @Test
  fun `test new test created`() {
    val taskFileName = "Task.kt"
    courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask {
          taskFile(taskFileName, "")
        }
      }
    }
    configureByTaskFile(1, 1, taskFileName)

    val taskFile = findFile("lesson1/task1")

    mockkConstructor(NewItemSimplePopupPanel::class)
    every { anyConstructed<NewItemSimplePopupPanel>().applyAction = any() } answers {
      (firstArg() as Consumer<in InputEvent>).consume(
        KeyEvent((self as NewItemSimplePopupPanel), 0, 0, 0, KeyEvent.VK_ENTER, KeyEvent.VK_ENTER.toChar()))
    }
    every { anyConstructed<NewItemSimplePopupPanel>().textField.text = any() } answers { nothing }
    every { anyConstructed<NewItemSimplePopupPanel>().textField.text } returns "myAwesomeTestName"

    testAction(CodeforcesCreateTestAction.ACTION_ID, dataContext(taskFile))

    fileTree {
      dir("lesson1/task1") {
        dir("testData") {
          dir("myAwesomeTestName") {
            file("input.txt")
            file("output.txt")
          }
        }
        file("Task.kt")
        file("task.html")
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

}