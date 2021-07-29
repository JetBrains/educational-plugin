package com.jetbrains.edu.coursecreator.actions.delete

import com.intellij.ide.actions.DeleteAction
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.withEduTestDialog

class CCDeleteActionTest : EduActionTestCase() {

  fun `test delete task`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1")
        eduTask("task2")
      }
    }

    val taskFile = findFile("lesson1/task1")

    val testDialog = TestDeleteDialog()
    withEduTestDialog(testDialog) {
      testAction(dataContext(taskFile), DeleteAction())
    }.checkWasShown()

    fileTree {
      dir("lesson1") {
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test delete lesson`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1")
      }
      lesson {
        eduTask("task2")
      }
    }

    val lessonFile = findFile("lesson1")
    withEduTestDialog(EduTestDialog()) {
      testAction(dataContext(lessonFile), DeleteAction())
    }

    fileTree {
      dir("lesson2") {
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test delete section`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
      lesson("lesson2") {
        eduTask("task2")
      }
    }

    val sectionFile = findFile("section1")
    
    val testDialog = TestDeleteDialog()
    withEduTestDialog(testDialog) {
      testAction(dataContext(sectionFile), DeleteAction())
    }.checkWasShown()

    fileTree {
      dir("lesson2") {
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  class TestDeleteDialog(
    private val expectedTasks: List<Task> = emptyList(),
    private val notExpectedTasks: List<Task> = emptyList()
  ) : EduTestDialog() {

    override fun show(message: String): Int {
      val result = super.show(message)
      for (task in expectedTasks) {
        val taskMessageName = CCStudyItemDeleteProvider.taskMessageName(task)
        if (taskMessageName !in message) {
          error("Can't find `$taskMessageName` in dialog message: `$message`")
        }
      }
      for (task in notExpectedTasks) {
        val taskMessageName = CCStudyItemDeleteProvider.taskMessageName(task)
        if (taskMessageName in message) {
          error("`$taskMessageName` isn't expected in dialog message: `$message`")
        }
      }
      return result
    }
  }
}
