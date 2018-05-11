package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.sections.CCWrapWithSection
import junit.framework.TestCase

class CCWrapInSectionTest : CCActionTestCase() {

  fun `test wrap consecutive lessons`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson()
      lesson()
      lesson()
    }
    Messages.setTestInputDialog { "section1" }
    course.courseMode = CCUtils.COURSE_MODE
    val lesson2 = LightPlatformTestCase.getSourceRoot().findChild("lesson2")
    val lesson3 = LightPlatformTestCase.getSourceRoot().findChild("lesson3")
    testAction(dataContext(arrayOf(lesson2!!, lesson3!!)), CCWrapWithSection())
    TestCase.assertEquals(3, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(2, section!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson4")!!.index)
  }

  fun `test wrap random lessons`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson()
      lesson()
      lesson()
      lesson()
    }
    Messages.setTestInputDialog { "section1" }
    val lesson2 = LightPlatformTestCase.getSourceRoot().findChild("lesson2")
    val lesson4 = LightPlatformTestCase.getSourceRoot().findChild("lesson4")
    testAction(dataContext(arrayOf(lesson2!!, lesson4!!)), CCWrapWithSection())
    TestCase.assertEquals(4, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson3")!!.index)
    TestCase.assertEquals(4, course.getLesson("lesson5")!!.index)
    TestCase.assertEquals(1, section.getLesson("lesson2")!!.index)
    TestCase.assertEquals(2, section.getLesson("lesson4")!!.index)
  }

  fun `test wrap one lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson()
      lesson()
      lesson()
      lesson()
    }
    Messages.setTestInputDialog { "section1" }
    val lesson2 = LightPlatformTestCase.getSourceRoot().findChild("lesson2")
    testAction(dataContext(arrayOf(lesson2!!)), CCWrapWithSection())
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
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson()
      lesson()
    }
    Messages.setTestInputDialog { "section1" }
    val lesson1 = LightPlatformTestCase.getSourceRoot().findChild("lesson1")
    val lesson2 = LightPlatformTestCase.getSourceRoot().findChild("lesson2")
    val lesson3 = LightPlatformTestCase.getSourceRoot().findChild("lesson3")
    testAction(dataContext(arrayOf(lesson1!!, lesson2!!, lesson3!!)), CCWrapWithSection())
    TestCase.assertEquals(1, course.items.size)
    val section = course.getSection("section1")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(1, section!!.index)
    TestCase.assertEquals(1, section.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, section.getLesson("lesson3")!!.index)
  }
}
