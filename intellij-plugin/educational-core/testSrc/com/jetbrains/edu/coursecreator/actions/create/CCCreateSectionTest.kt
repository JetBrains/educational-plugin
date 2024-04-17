package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateSection
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class CCCreateSectionTest : EduActionTestCase() {

  @Test
  fun `test create section in course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    withMockCreateStudyItemUi(MockNewStudyItemUi("section1")) {
      testAction(CCCreateSection.ACTION_ID, dataContext(LightPlatformTestCase.getSourceRoot()))
    }

    assertEquals(2, course.items.size)
    val section = course.getSection("section1")
    assertNotNull(section)
    assertEquals(2, section!!.index)
  }

  @Test
  fun `test create section after lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    withMockCreateStudyItemUi(MockNewStudyItemUi("section2", 2)) {
      testAction(CCCreateSection.ACTION_ID, dataContext(lessonFile))
    }
    assertEquals(4, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getSection("section2")!!.index)
    assertEquals(3, course.getLesson("lesson2")!!.index)
    assertEquals(4, course.getSection("section3")!!.index)
  }

  @Test
  fun `test create section before lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    withMockCreateStudyItemUi(MockNewStudyItemUi("section2", 2)) {
      testAction(CCCreateSection.ACTION_ID, dataContext(lessonFile))
    }
    assertEquals(4, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getSection("section2")!!.index)
    assertEquals(3, course.getLesson("lesson2")!!.index)
    assertEquals(4, course.getSection("section3")!!.index)
  }

  @Test
  fun `test create section before section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    withMockCreateStudyItemUi(MockNewStudyItemUi("section1", 2)) {
      testAction(CCCreateSection.ACTION_ID, dataContext(sectionFile))
    }
    assertEquals(4, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getSection("section1")!!.index)
    assertEquals(3, course.getSection("section2")!!.index)
    assertEquals(4, course.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test create section not available inside lesson`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = findFile("lesson1/task1")
    testAction(CCCreateSection.ACTION_ID, dataContext(sourceVFile), shouldBeEnabled = false)
  }

  @Test
  fun `test create section after section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    withMockCreateStudyItemUi(MockNewStudyItemUi("section1", 3)) {
      testAction(CCCreateSection.ACTION_ID, dataContext(sectionFile))
    }
    assertEquals(4, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getSection("section2")!!.index)
    assertEquals(3, course.getSection("section1")!!.index)
    assertEquals(4, course.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test section creation is not available from inner directories`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      section("section1") {
        lesson("lesson1") {
          eduTask("lesson1") {
            taskFile("taskFile2.txt")
          }
        }
      }
    }

    val innerLessonDir = findFile("section1/lesson1")
    val taskDir = findFile("section1/lesson1/lesson1")
    withMockCreateStudyItemUi(MockNewStudyItemUi("section2")) {
      testAction(CCCreateSection.ACTION_ID, dataContext(innerLessonDir), shouldBeEnabled = false)
    }
    withMockCreateStudyItemUi(MockNewStudyItemUi("section3")) {
      testAction(CCCreateSection.ACTION_ID, dataContext(taskDir), shouldBeEnabled = false)
    }
  }

  @Test
  fun `test suggest name for new section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {}
    }

    fun assertLessons(vararg names: String) {
      val actualNames = course.items.map { it.name }
      assertEquals(listOf(*names), actualNames)
    }

    fun createSection(contextFolder: String, suggestedIndex: Int? = null, suggestedName: String? = null) {
      withMockCreateStudyItemUi(MockNewStudyItemUi(suggestedName, suggestedIndex)) {
        testAction(CCCreateSection.ACTION_ID, dataContext(findFile(contextFolder)))
      }
    }

    createSection("")
    assertLessons("section1", "section2")
    createSection("", suggestedName = "section4")
    assertLessons("section1", "section2", "section4")
    createSection("")
    assertLessons("section1", "section2", "section4", "section5")
    createSection("section4")
    assertLessons("section1", "section2", "section4", "section6", "section5")
    createSection("section4", suggestedIndex = 5)
    assertLessons("section1", "section2", "section4", "section6", "section7", "section5")
  }
}
