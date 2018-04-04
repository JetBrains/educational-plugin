package com.jetbrains.edu.learning.actions

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import junit.framework.TestCase

class NavigateTaskTest : EduTestCase() {
  fun `test next task`() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    myFixture.testAction(NextTaskAction())
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile
    val taskFile = EduUtils.getTaskFile(myFixture.project, currentFile!!)
    val task = taskFile!!.task
    TestCase.assertEquals(2, task.index)
  }

  fun `test previous task`() {
    configureByTaskFile(1, 2, "taskFile2.txt")
    myFixture.testAction(PreviousTaskAction())
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile
    val taskFile = EduUtils.getTaskFile(myFixture.project, currentFile!!)
    val task = taskFile!!.task
    TestCase.assertEquals(1, task.index)
  }

  fun `test next lesson`() {
    configureByTaskFile(1, 2, "taskFile2.txt")
    myFixture.testAction(NextTaskAction())
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile
    val taskFile = EduUtils.getTaskFile(myFixture.project, currentFile!!)
    val task = taskFile!!.task
    TestCase.assertEquals(1, task.index)
    val lesson = task.lesson
    TestCase.assertEquals(2, lesson.index)
  }

  fun `test previous lesson`() {
    configureByTaskFile(2, 1, "taskFile1.txt")
    myFixture.testAction(PreviousTaskAction())
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile
    val taskFile = EduUtils.getTaskFile(myFixture.project, currentFile!!)
    val task = taskFile!!.task
    TestCase.assertEquals(2, task.index)
    val lesson = task.lesson
    TestCase.assertEquals(1, lesson.index)
  }

  fun `test last task`() {
    configureByTaskFile(2, 2, "taskFile2.txt")
    myFixture.testAction(NextTaskAction())
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile
    val taskFile = EduUtils.getTaskFile(myFixture.project, currentFile!!)
    val task = taskFile!!.task
    TestCase.assertEquals(2, task.index)
    val lesson = task.lesson
    TestCase.assertEquals(2, lesson.index)
  }

  fun `test first task`() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    myFixture.testAction(PreviousTaskAction())
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile
    val taskFile = EduUtils.getTaskFile(myFixture.project, currentFile!!)
    val task = taskFile!!.task
    TestCase.assertEquals(1, task.index)
    val lesson = task.lesson
    TestCase.assertEquals(1, lesson.index)
  }

  override fun createCourse() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }
  }

}
