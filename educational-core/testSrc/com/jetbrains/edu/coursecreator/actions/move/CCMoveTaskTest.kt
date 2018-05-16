package com.jetbrains.edu.coursecreator.actions.move

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.handlers.CCTaskMoveHandlerDelegate
import com.jetbrains.edu.learning.courseFormat.StudyItem
import junit.framework.TestCase

class CCMoveTaskTest : CCActionTestCase() {

  fun `test move to another lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
        eduTask()
      }
      lesson {
        eduTask("task2")
      }
    }
    val sourceVFile = findFile("lesson1/task1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerDelegate()
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val lesson1 = course.getLesson("lesson1")
    val lesson2 = course.getLesson("lesson2")
    TestCase.assertEquals(1, lesson1!!.taskList.size)
    TestCase.assertEquals(2, lesson2!!.taskList.size)

    TestCase.assertEquals(1, lesson1.getTask("task2")!!.index)

    TestCase.assertEquals(1, lesson2.getTask("task2")!!.index)
    TestCase.assertEquals(2, lesson2.getTask("task1")!!.index)
  }

  fun `test move task with custom name to another lesson`() {
    val customTaskName = "Custom Task Name"
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask(customTaskName)
        eduTask()
      }
      lesson {
        eduTask()
      }
    }
    val sourceVFile = findFile("lesson1/$customTaskName")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerDelegate()
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val lesson1 = course.getLesson("lesson1")
    val lesson2 = course.getLesson("lesson2")
    TestCase.assertEquals(1, lesson1!!.taskList.size)
    TestCase.assertEquals(2, lesson2!!.taskList.size)

    TestCase.assertEquals(1, lesson1.getTask("task2")!!.index)

    TestCase.assertEquals(1, lesson2.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson2.getTask(customTaskName)!!.index)
  }

  fun `test move after task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
        eduTask()
        eduTask()
        eduTask()
      }
    }
    val sourceVFile = findFile("lesson1/task2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson1/task3")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerTest(1)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val lesson1 = course.getLesson("lesson1")
    TestCase.assertEquals(4, lesson1!!.taskList.size)

    TestCase.assertEquals(1, lesson1.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson1.getTask("task3")!!.index)
    TestCase.assertEquals(3, lesson1.getTask("task2")!!.index)
    TestCase.assertEquals(4, lesson1.getTask("task4")!!.index)
  }

  fun `test move before task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
        eduTask()
        eduTask()
        eduTask()
      }
    }
    val sourceVFile = findFile("lesson1/task1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson1/task3")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val lesson1 = course.getLesson("lesson1")
    TestCase.assertEquals(4, lesson1!!.taskList.size)

    TestCase.assertEquals(1, lesson1.getTask("task2")!!.index)
    TestCase.assertEquals(2, lesson1.getTask("task1")!!.index)
    TestCase.assertEquals(3, lesson1.getTask("task3")!!.index)
    TestCase.assertEquals(4, lesson1.getTask("task4")!!.index)
  }

  fun `test move before task in another lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
        eduTask()
      }
      lesson {
        eduTask("task3")
        eduTask("task4")
      }
    }
    val sourceVFile = findFile("lesson1/task1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson2/task3")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val lesson1 = course.getLesson("lesson1")
    val lesson2 = course.getLesson("lesson2")
    TestCase.assertEquals(1, lesson1!!.taskList.size)
    TestCase.assertEquals(3, lesson2!!.taskList.size)

    TestCase.assertEquals(1, lesson1.getTask("task2")!!.index)

    TestCase.assertEquals(1, lesson2.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson2.getTask("task3")!!.index)
    TestCase.assertEquals(3, lesson2.getTask("task4")!!.index)
  }

  fun `test move after task in another lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
        eduTask()
      }
      lesson {
        eduTask("task3")
        eduTask("task4")
      }
    }
    val sourceVFile = findFile("lesson1/task1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson2/task3")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerTest(1)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val lesson1 = course.getLesson("lesson1")
    val lesson2 = course.getLesson("lesson2")
    TestCase.assertEquals(1, lesson1!!.taskList.size)
    TestCase.assertEquals(3, lesson2!!.taskList.size)

    TestCase.assertEquals(1, lesson1.getTask("task2")!!.index)

    TestCase.assertEquals(1, lesson2.getTask("task3")!!.index)
    TestCase.assertEquals(2, lesson2.getTask("task1")!!.index)
    TestCase.assertEquals(3, lesson2.getTask("task4")!!.index)
  }

  internal inner class CCTaskMoveHandlerTest(private val myDelta: Int) : CCTaskMoveHandlerDelegate() {
    override fun getDelta(project: Project, targetItem: StudyItem): Int {
      return myDelta
    }
  }
}
