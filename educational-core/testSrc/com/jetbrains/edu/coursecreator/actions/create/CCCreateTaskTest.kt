package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.courseFormat.StudyItem
import junit.framework.TestCase

class CCCreateTaskTest : CCActionTestCase() {

  fun `test create task in lesson`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    Messages.setTestInputDialog { "task2" }
    val lessonName = "lesson1"
    val lessonFile = LightPlatformTestCase.getSourceRoot().findChild(lessonName)

    testAction(dataContext(lessonFile!!), CCCreateTask())
    TestCase.assertEquals(2, course.lessons[0].taskList.size)
  }

  fun `test create task in lesson in section`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    Messages.setTestInputDialog { "task2" }
    val lessonName = "lesson1"
    val sectionName = "section1"
    val sectionFile = LightPlatformTestCase.getSourceRoot().findChild(sectionName)
    val lessonFile = sectionFile!!.findChild(lessonName)

    testAction(dataContext(lessonFile!!), CCCreateTask())
    TestCase.assertEquals(2, course.sections[0].lessons[0].taskList.size)
  }

  fun `test create task in empty lesson`() {
    val course = courseWithFiles {
      lesson {
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lessonName = "lesson1"
    val lessonFile = LightPlatformTestCase.getSourceRoot().findChild(lessonName)
    Messages.setTestInputDialog { "task1" }
    testAction(dataContext(lessonFile!!), CCCreateTask())
    TestCase.assertEquals(1, course.lessons[0].taskList.size)
  }

  fun `test create task after task`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lessonName = "lesson1"
    val lessonFile = LightPlatformTestCase.getSourceRoot().findChild(lessonName)
    val taskFile = lessonFile!!.findChild("task1")

    testAction(dataContext(taskFile!!), CCCreateTaskTest("task01", 2))
    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1").index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task before task`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lessonName = "lesson1"
    val lessonFile = LightPlatformTestCase.getSourceRoot().findChild(lessonName)
    val taskFile = lessonFile!!.findChild("task2")

    testAction(dataContext(taskFile!!), CCCreateTaskTest("task01", 2))
    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1").index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task after task in section`() {
    val course = courseWithFiles {
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
    course.courseMode = CCUtils.COURSE_MODE
    val sectionName = "section1"
    val sectionFile = LightPlatformTestCase.getSourceRoot().findChild(sectionName)
    val lessonFile = sectionFile!!.findChild("lesson1")
    val taskFile = lessonFile!!.findChild("task1")

    testAction(dataContext(taskFile!!), CCCreateTaskTest("task01", 2))
    val lesson = course.sections[0].lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1").index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task not available on course`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = LightPlatformTestCase.getSourceRoot()
    course.courseMode = CCUtils.COURSE_MODE
    val action = CCCreateTask()
    val event = TestActionEvent(dataContext(sourceVFile!!), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }

  fun `test create task not available on section`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val sourceVFile = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "section1")
    course.courseMode = CCUtils.COURSE_MODE
    val action = CCCreateTask()
    val event = TestActionEvent(dataContext(sourceVFile!!), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }

  internal inner class CCCreateTaskTest(private val myName: String, private val myIndex: Int) : CCCreateTask() {
    override fun getItemNameIndex(thresholdItem: StudyItem,
                                  project: Project,
                                  sourceDirectory: VirtualFile): Pair<String, Int>? {
      return Pair.create(myName, myIndex)
    }
  }
}
