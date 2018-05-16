package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.sections.CCCreateSection
import junit.framework.TestCase

class CCCreateSectionTest : CCActionTestCase() {

  fun `test create section in course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    Messages.setTestInputDialog { "section1" }
    testAction(dataContext(LightPlatformTestCase.getSourceRoot()), CCCreateSection())
    TestCase.assertEquals(2, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(2, section!!.index)
  }

  fun `test create section after lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile2.txt")
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
    val lessonFile = findFile("lesson1")
    testAction(dataContext(lessonFile), CCTestCreateSection("section2", 2))
    TestCase.assertEquals(4, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section2")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(4, course.getSection("section3")!!.index)
  }

  fun `test create section before lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile2.txt")
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
    val lessonFile = findFile("lesson2")
    testAction(dataContext(lessonFile), CCTestCreateSection("section2", 2))
    TestCase.assertEquals(4, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section2")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(4, course.getSection("section3")!!.index)
  }

  fun `test create section before section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
      lesson {
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }
    val sectionFile = findFile("section2")
    testAction(dataContext(sectionFile), CCTestCreateSection("section1", 2))
    TestCase.assertEquals(4, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section1")!!.index)
    TestCase.assertEquals(3, course.getSection("section2")!!.index)
    TestCase.assertEquals(4, course.getLesson("lesson2")!!.index)
  }

  fun `test create section not available inside lesson`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = findFile("lesson1/task1")
    val action = CCCreateSection()
    val event = TestActionEvent(dataContext(sourceVFile), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }

  fun `test create section after section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
      lesson {
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }
    val sectionFile = findFile("section2")
    testAction(dataContext(sectionFile), CCTestCreateSection("section1", 3))
    TestCase.assertEquals(4, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section2")!!.index)
    TestCase.assertEquals(3, course.getSection("section1")!!.index)
    TestCase.assertEquals(4, course.getLesson("lesson2")!!.index)
  }
}
