package com.jetbrains.edu.coursecreator.actions.create

import com.jetbrains.edu.coursecreator.actions.studyItem.CCWrapWithSection
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestInputDialog
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withEduTestDialog
import junit.framework.TestCase

class CCWrapInSectionTest : EduActionTestCase() {

  fun `test wrap consecutive lessons`() {
    val course = courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
      lesson()
      lesson()
      lesson()
      lesson()
    }
    course.courseMode = CourseMode.COURSE_MODE
    val lesson2 = findFile("lesson2")
    val lesson3 = findFile("lesson3")
    withEduTestDialog(EduTestInputDialog("section1")) {
      testAction(CCWrapWithSection.ACTION_ID, dataContext(arrayOf(lesson2, lesson3)))
    }
    TestCase.assertEquals(3, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(2, section!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson4")!!.index)
  }

  fun `test wrap random lessons`() {
    courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
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

  fun `test wrap one lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
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
    TestCase.assertEquals(5, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson3")!!.index)
    TestCase.assertEquals(4, course.getLesson("lesson4")!!.index)
    TestCase.assertEquals(5, course.getLesson("lesson5")!!.index)
    TestCase.assertEquals(1, section.getLesson("lesson2")!!.index)
  }

  fun `test all lessons`() {
    val course = courseWithFiles(courseMode = CourseMode.COURSE_MODE) {
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
    TestCase.assertEquals(1, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(1, section!!.index)
    TestCase.assertEquals(1, section.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, section.getLesson("lesson3")!!.index)
  }
}
