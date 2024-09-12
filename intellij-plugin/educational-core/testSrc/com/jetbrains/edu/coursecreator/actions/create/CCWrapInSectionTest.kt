package com.jetbrains.edu.coursecreator.actions.create

import com.jetbrains.edu.coursecreator.actions.studyItem.CCWrapWithSection
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestInputDialog
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withEduTestDialog
import org.junit.Test

class CCWrapInSectionTest : EduActionTestCase() {

  @Test
  fun `test wrap consecutive lessons`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      lesson()
      lesson()
      lesson()
    }
    course.courseMode = CourseMode.EDUCATOR
    val lesson2 = findFile("lesson2")
    val lesson3 = findFile("lesson3")
    withEduTestDialog(EduTestInputDialog("section1")) {
      testAction(CCWrapWithSection.ACTION_ID, dataContext(arrayOf(lesson2, lesson3)))
    }
    assertEquals(3, course.items.size)
    val section = course.getSection("section1")
    assertNotNull(section)
    assertEquals(2, section!!.index)
    assertEquals(3, course.getLesson("lesson4")!!.index)
  }

  @Test
  fun `test wrap random lessons`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      lesson()
      lesson()
      lesson()
      lesson()
    }
    val lesson2 = findFile("lesson2")
    val lesson4 = findFile("lesson4")

    val context = dataContext(arrayOf(lesson2, lesson4))
    testAction(CCWrapWithSection.ACTION_ID, context, shouldBeEnabled = false)
  }

  @Test
  fun `test wrap one lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      lesson()
      lesson()
      lesson()
      lesson()
    }
    val lesson2 = findFile("lesson2")
    withEduTestDialog(EduTestInputDialog("section1")) {
      testAction(CCWrapWithSection.ACTION_ID, dataContext(arrayOf(lesson2)))
    }
    assertEquals(5, course.items.size)
    val section = course.getSection("section1")
    assertNotNull(section)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, section!!.index)
    assertEquals(3, course.getLesson("lesson3")!!.index)
    assertEquals(4, course.getLesson("lesson4")!!.index)
    assertEquals(5, course.getLesson("lesson5")!!.index)
    assertEquals(1, section.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test all lessons`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      lesson()
      lesson()
    }
    val lesson1 = findFile("lesson1")
    val lesson2 = findFile("lesson2")
    val lesson3 = findFile("lesson3")
    withEduTestDialog(EduTestInputDialog("section1")) {
      testAction(CCWrapWithSection.ACTION_ID, dataContext(arrayOf(lesson1, lesson2, lesson3)))
    }
    assertEquals(1, course.items.size)
    val section = course.getSection("section1")
    assertNotNull(section)
    assertEquals(1, section!!.index)
    assertEquals(1, section.getLesson("lesson1")!!.index)
    assertEquals(2, section.getLesson("lesson2")!!.index)
    assertEquals(3, section.getLesson("lesson3")!!.index)
  }
  
  @Test
  fun `test lesson wrapping is available only for top level lessons`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1")
      section("section1") {
        lesson("lesson1") {
          eduTask("lesson1")
        }
      }
    }
    val innerLessonDir = findFile("section1/lesson1")
    val taskDir = findFile("section1/lesson1/lesson1")
    withEduTestDialog(EduTestInputDialog("section2")) {
      testAction(CCWrapWithSection.ACTION_ID, dataContext(arrayOf(innerLessonDir)), shouldBeEnabled = false)
    }
    withEduTestDialog(EduTestInputDialog("section3")) {
      testAction(CCWrapWithSection.ACTION_ID, dataContext(arrayOf(taskDir)), shouldBeEnabled = false)
    }
  }

  @Test
  fun `test task records in framework lesson remain correct after lesson wrap into section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.kt")
        }
      }
    }

    val lesson = course.lessons.first()
    val task = lesson.taskList.first()

    val manager = CCFrameworkLessonManager.getInstance(project)

    manager.updateRecord(task, 1)

    val lessonDir = findFile("lesson1")
    withEduTestDialog(EduTestInputDialog("section1")) {
      testAction(CCWrapWithSection.ACTION_ID, dataContext(arrayOf(lessonDir)), shouldBeEnabled = true)
    }

    assertEquals(1, manager.getRecord(task))
  }
}
