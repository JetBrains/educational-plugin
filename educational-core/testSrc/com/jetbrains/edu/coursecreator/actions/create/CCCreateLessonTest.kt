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
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import junit.framework.TestCase

class CCCreateLessonTest : CCActionTestCase() {

  fun `test create lesson in course`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    Messages.setTestInputDialog { "lesson2" }
    testAction(dataContext(LightPlatformTestCase.getSourceRoot()), CCCreateLesson())
    TestCase.assertEquals(2, course.lessons.size)
  }

  fun `test create lesson in section`() {
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
    Messages.setTestInputDialog { "lesson2" }
    val sectionName = "section1"
    val sectionFile = LightPlatformTestCase.getSourceRoot().findChild(sectionName)
    testAction(dataContext(sectionFile!!), CCCreateLesson())
    TestCase.assertEquals(2, course.getSection(sectionName)!!.lessons.size)
  }

  fun `test create lesson between lessons in course`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      lesson(name="lesson3") {
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lessonName = "lesson1"
    val lessonFile = LightPlatformTestCase.getSourceRoot().findChild(lessonName)
    testAction(dataContext(lessonFile!!), CCCreateLessonTest("lesson2", 2))
    TestCase.assertEquals(3, course.lessons.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson3")!!.index)
  }

  fun `test create lesson before section in course`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      section {
        lesson {
          eduTask {
            taskFile("taskFile2.txt")
          }
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lessonName = "lesson1"
    val lessonFile = LightPlatformTestCase.getSourceRoot().findChild(lessonName)
    testAction(dataContext(lessonFile!!), CCCreateLessonTest("lesson2", 2))
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, course.getSection("section2")!!.index)
  }

  fun `test create lesson after section in course`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lessonName = "lesson1"
    val lessonFile = LightPlatformTestCase.getSourceRoot().findChild(lessonName)
    testAction(dataContext(lessonFile!!), CCCreateLessonTest("lesson01", 2))
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getSection("section1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson01")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson1")!!.index)
  }

  fun `test create lesson between lessons in section`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
        lesson(name="lesson3") {
          eduTask {
            taskFile("taskFile2.txt")
          }
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val sectionName = "section1"
    val sectionFile = LightPlatformTestCase.getSourceRoot().findChild(sectionName)
    val lessonFile = sectionFile!!.findChild("lesson1")
    testAction(dataContext(lessonFile!!), CCCreateLessonTest("lesson2", 2))
    val section = course.getSection(sectionName)
    TestCase.assertEquals(3, section!!.items.size)
    TestCase.assertEquals(1, section.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, section.getLesson("lesson3")!!.index)
  }

  fun `test create lesson not available inside lesson`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "lesson1", "task1")
    course.courseMode = CCUtils.COURSE_MODE
    val action = CCCreateLesson()
    val event = TestActionEvent(dataContext(sourceVFile!!), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }

  internal inner class CCCreateLessonTest(private val myName: String, private val myIndex: Int) : CCCreateLesson() {
    override fun getItemNameIndex(thresholdItem: StudyItem,
                                  project: Project,
                                  sourceDirectory: VirtualFile): Pair<String, Int>? {
      return Pair.create(myName, myIndex)
    }
  }
}
