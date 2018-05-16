package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import junit.framework.TestCase

class CCCreateTaskTest : CCActionTestCase() {

  fun `test create task in lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    Messages.setTestInputDialog { "task2" }
    val lessonFile = findFile("lesson1")

    testAction(dataContext(lessonFile), CCCreateTask())
    TestCase.assertEquals(2, course.lessons[0].taskList.size)
  }

  fun `test create task in lesson in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    Messages.setTestInputDialog { "task2" }
    val lessonFile = findFile("section1/lesson1")

    testAction(dataContext(lessonFile), CCCreateTask())
    TestCase.assertEquals(2, course.sections[0].lessons[0].taskList.size)
  }

  fun `test create task in empty lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
    }
    val lessonFile = findFile("lesson1")
    Messages.setTestInputDialog { "task1" }
    testAction(dataContext(lessonFile), CCCreateTask())
    TestCase.assertEquals(1, course.lessons[0].taskList.size)
  }

  fun `test create task after task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val taskFile = findFile("lesson1/task1")

    testAction(dataContext(taskFile), CCTestCreateTask("task01", 2))
    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task before task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val taskFile = findFile("lesson1/task2")

    testAction(dataContext(taskFile), CCTestCreateTask("task01", 2))
    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task before task with custom name`() {
    val customTaskName = "Custom Task Name"
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask(customTaskName) {
          taskFile("taskFile1.txt")
        }
      }
    }
    val taskFile = findFile("lesson1/$customTaskName")

    testAction(dataContext(taskFile), CCTestCreateTask("task01", 2))
    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask(customTaskName)!!.index)
  }

  fun `test create task after task in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val taskFile = findFile("section1/lesson1/task1")

    testAction(dataContext(taskFile), CCTestCreateTask("task01", 2))
    val lesson = course.sections[0].lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task not available on course`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = LightPlatformTestCase.getSourceRoot()
    val action = CCCreateTask()
    val event = TestActionEvent(dataContext(sourceVFile!!), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }

  fun `test create task not available on section`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val sourceVFile = findFile("section1")
    val action = CCCreateTask()
    val event = TestActionEvent(dataContext(sourceVFile), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }


}
