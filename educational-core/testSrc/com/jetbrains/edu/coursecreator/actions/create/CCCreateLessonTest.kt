package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import junit.framework.TestCase

class CCCreateLessonTest : EduActionTestCase() {

  fun `test create lesson in course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2")) {
      testAction(dataContext(LightPlatformTestCase.getSourceRoot()), CCCreateLesson())
    }
    TestCase.assertEquals(2, course.lessons.size)
  }

  fun `test create lesson in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val sectionName = "section1"
    val sectionFile = findFile(sectionName)
    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2")) {
      testAction(dataContext(sectionFile), CCCreateLesson())
    }
    TestCase.assertEquals(2, course.getSection(sectionName)!!.lessons.size)
  }

  fun `test create lesson between lessons in course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      lesson(name = "lesson3") {
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }
    val lessonFile = findFile("lesson1")
    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2", 2)) {
      testAction(dataContext(lessonFile), CCCreateLesson())
    }
    TestCase.assertEquals(3, course.lessons.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson3")!!.index)
  }

  fun `test create lesson before section in course`() {
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
    }
    val lessonFile = findFile("lesson1")
    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2", 2)) {
      testAction(dataContext(lessonFile), CCCreateLesson())
    }
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, course.getSection("section2")!!.index)
  }

  fun `test create lesson after section in course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
    val lessonFile = findFile("lesson1")
    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson01", 2)) {
      testAction(dataContext(lessonFile), CCCreateLesson())
    }
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getSection("section1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson01")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson1")!!.index)
  }

  fun `test create lesson between lessons in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
        lesson(name = "lesson3") {
          eduTask {
            taskFile("taskFile2.txt")
          }
        }
      }
    }
    val sectionName = "section1"
    val lessonFile = findFile("$sectionName/lesson1")
    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2", 2)) {
      testAction(dataContext(lessonFile), CCCreateLesson())
    }
    val section = course.getSection(sectionName)
    TestCase.assertEquals(3, section!!.items.size)
    TestCase.assertEquals(1, section.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, section.getLesson("lesson3")!!.index)
  }

  fun `test create lesson not available inside lesson`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = findFile("lesson1/task1")
    val action = CCCreateLesson()
    val event = TestActionEvent(dataContext(sourceVFile), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }
}
