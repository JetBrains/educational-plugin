package com.jetbrains.edu.coursecreator.actions.delete

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.sections.CCRemoveSection
import com.jetbrains.edu.learning.fileTree
import junit.framework.TestCase

class CCUnWrapSectionTest : CCActionTestCase() {

  fun `test unwrap lessons`() {
    val course = courseWithFiles {
      lesson {
      }
      section {
        lesson("lesson2", {})
        lesson("lesson3", {})
      }
      lesson("lesson4", false, {})
    }
    course.courseMode = CCUtils.COURSE_MODE
    val section2 = LightPlatformTestCase.getSourceRoot().findChild("section2")
    testAction(dataContext(arrayOf(section2!!)), CCRemoveSection())
    TestCase.assertEquals(4, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNull(section)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson3")!!.index)
    TestCase.assertEquals(4, course.getLesson("lesson4")!!.index)
  }

  fun `test unwrap lessons tree structure`() {
    val course = courseWithFiles {
      lesson {
      }
      section {
        lesson("lesson2", {})
        lesson("lesson3", {})
      }
      lesson("lesson4", false, {})
    }
    course.courseMode = CCUtils.COURSE_MODE
    val section2 = LightPlatformTestCase.getSourceRoot().findChild("section2")
    testAction(dataContext(arrayOf(section2!!)), CCRemoveSection())
    val expectedFileTree = fileTree {
      dir("lesson1") {}
      dir("lesson2") {}
      dir("lesson3") {}
      dir("lesson4") {}
    }
    expectedFileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test course has the same named lesson`() {
    val course = courseWithFiles {
      lesson {}
      section {
        lesson("lesson1", {})
        lesson("lesson2", {})
      }
      lesson {}
    }
    course.courseMode = CCUtils.COURSE_MODE
    val section2 = LightPlatformTestCase.getSourceRoot().findChild("section2")
    try {
      testAction(dataContext(arrayOf(section2!!)), CCRemoveSection())
      TestCase.fail("Expected failed to move lesson out message")
    }
    catch (e: Throwable) {}

    TestCase.assertEquals(3, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section2")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)
  }
}
