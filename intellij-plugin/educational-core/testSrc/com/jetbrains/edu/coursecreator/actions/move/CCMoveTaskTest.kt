package com.jetbrains.edu.coursecreator.actions.move

import com.jetbrains.edu.learning.actions.move.MoveTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class CCMoveTaskTest : MoveTestBase() {

  @Test
  fun `test move to another lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask()
        eduTask()
      }
      lesson {
        eduTask("task2")
      }
    }
    val sourceDir = findPsiDirectory("lesson1/task1")
    val targetDir = findPsiDirectory("lesson2")

    doMoveAction(course, sourceDir, targetDir)

    val lesson1 = course.getLesson("lesson1")!!
    val lesson2 = course.getLesson("lesson2")!!
    assertEquals(1, lesson1.taskList.size)
    assertEquals(2, lesson2.taskList.size)

    assertEquals(1, lesson1.getTask("task2")!!.index)

    assertEquals(1, lesson2.getTask("task2")!!.index)
    assertEquals(2, lesson2.getTask("task1")!!.index)
  }

  // EDU-2467
  @Test
  fun `test move from lesson with the same name to another lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("lesson1")
        eduTask("task2")
      }
      lesson("lesson2") {
        eduTask("task2")
      }
    }
    val sourceDir = findPsiDirectory("lesson1/lesson1")
    val targetDir = findPsiDirectory("lesson2")
    doMoveAction(course, sourceDir, targetDir, delta = 1)
    val lesson1 = course.getLesson("lesson1")
    val lesson2 = course.getLesson("lesson2")
    assertEquals(1, lesson1!!.taskList.size)
    assertEquals(2, lesson2!!.taskList.size)

    assertEquals(1, lesson1.getTask("task2")!!.index)

    assertEquals(1, lesson2.getTask("task2")!!.index)
    assertEquals(2, lesson2.getTask("lesson1")!!.index)
  }

  @Test
  fun `test move task with custom name to another lesson`() {
    val customTaskName = "Custom Task Name"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customTaskName)
        eduTask()
      }
      lesson {
        eduTask()
      }
    }
    val sourceDir = findPsiDirectory("lesson1/$customTaskName")
    val targetDir = findPsiDirectory("lesson2")

    doMoveAction(course, sourceDir, targetDir)

    val lesson1 = course.getLesson("lesson1")!!
    val lesson2 = course.getLesson("lesson2")!!

    assertEquals(1, lesson1.taskList.size)
    assertEquals(2, lesson2.taskList.size)

    assertEquals(1, lesson1.getTask("task2")!!.index)

    assertEquals(1, lesson2.getTask("task1")!!.index)
    assertEquals(2, lesson2.getTask(customTaskName)!!.index)
  }

  @Test
  fun `test move after task`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask()
        eduTask()
        eduTask()
        eduTask()
      }
    }
    val sourceDir = findPsiDirectory("lesson1/task2")
    val targetDir = findPsiDirectory("lesson1/task3")

    doMoveAction(course, sourceDir, targetDir, delta = 1)

    val lesson1 = course.getLesson("lesson1")!!
    assertEquals(4, lesson1.taskList.size)

    assertEquals(1, lesson1.getTask("task1")!!.index)
    assertEquals(2, lesson1.getTask("task3")!!.index)
    assertEquals(3, lesson1.getTask("task2")!!.index)
    assertEquals(4, lesson1.getTask("task4")!!.index)
  }

  @Test
  fun `test move before task`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask()
        eduTask()
        eduTask()
        eduTask()
      }
    }
    val sourceDir = findPsiDirectory("lesson1/task1")
    val targetDir = findPsiDirectory("lesson1/task3")

    doMoveAction(course, sourceDir, targetDir, delta = 0)

    val lesson1 = course.getLesson("lesson1")!!
    assertEquals(4, lesson1.taskList.size)

    assertEquals(1, lesson1.getTask("task2")!!.index)
    assertEquals(2, lesson1.getTask("task1")!!.index)
    assertEquals(3, lesson1.getTask("task3")!!.index)
    assertEquals(4, lesson1.getTask("task4")!!.index)
  }

  @Test
  fun `test move before task in another lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask()
        eduTask()
      }
      lesson {
        eduTask("task3")
        eduTask("task4")
      }
    }
    val sourceDir = findPsiDirectory("lesson1/task1")
    val targetDir = findPsiDirectory("lesson2/task3")

    doMoveAction(course, sourceDir, targetDir, delta = 0)

    val lesson1 = course.getLesson("lesson1")!!
    val lesson2 = course.getLesson("lesson2")!!
    assertEquals(1, lesson1.taskList.size)
    assertEquals(3, lesson2.taskList.size)

    assertEquals(1, lesson1.getTask("task2")!!.index)

    assertEquals(1, lesson2.getTask("task1")!!.index)
    assertEquals(2, lesson2.getTask("task3")!!.index)
    assertEquals(3, lesson2.getTask("task4")!!.index)
  }

  @Test
  fun `test move after task in another lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask()
        eduTask()
      }
      lesson {
        eduTask("task3")
        eduTask("task4")
      }
    }
    val sourceDir = findPsiDirectory("lesson1/task1")
    val targetDir = findPsiDirectory("lesson2/task3")

    doMoveAction(course, sourceDir, targetDir, delta = 1)

    val lesson1 = course.getLesson("lesson1")!!
    val lesson2 = course.getLesson("lesson2")!!
    assertEquals(1, lesson1.taskList.size)
    assertEquals(3, lesson2.taskList.size)

    assertEquals(1, lesson1.getTask("task2")!!.index)

    assertEquals(1, lesson2.getTask("task3")!!.index)
    assertEquals(2, lesson2.getTask("task1")!!.index)
    assertEquals(3, lesson2.getTask("task4")!!.index)
  }
}
