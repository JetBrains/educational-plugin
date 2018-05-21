package com.jetbrains.edu.coursecreator.actions.delete

import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCVirtualFileListener
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.sections.CCRemoveSection
import com.jetbrains.edu.learning.fileTree
import junit.framework.TestCase

class CCUnWrapSectionTest : CCActionTestCase() {
  private lateinit var ccVirtualFileListener : VirtualFileListener
  override fun setUp() {
    super.setUp()
    ccVirtualFileListener = CCVirtualFileListener(project)
    VirtualFileManager.getInstance().addVirtualFileListener(ccVirtualFileListener)
  }

  override fun tearDown() {
    super.tearDown()
    VirtualFileManager.getInstance().removeVirtualFileListener(ccVirtualFileListener)
  }

  fun `test unwrap lessons`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson("lesson2")
        lesson("lesson3")
      }
      lesson("lesson4")
    }
    val section2 = findFile("section2")
    testAction(dataContext(arrayOf(section2)), CCRemoveSection())
    TestCase.assertEquals(4, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNull(section)
    for (i in 1..4) {
      TestCase.assertEquals(i, course.getLesson("lesson$i")!!.index)
    }
  }

  fun `test one lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson("lesson2")
      }
      lesson("lesson3")
    }
    val section2 = findFile("section2")
    testAction(dataContext(arrayOf(section2)), CCRemoveSection())
    TestCase.assertEquals(3, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNull(section)
    for (i in 1..3) {
      TestCase.assertEquals(i, course.getLesson("lesson$i")!!.index)
    }
  }

  fun `test with multiple lesson before and after`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson()
      lesson()
      section("section2") {
        lesson("lesson4")
        lesson("lesson5")
      }
      lesson("lesson6")
      lesson("lesson7")
    }
    val section2 = findFile("section2")
    testAction(dataContext(arrayOf(section2)), CCRemoveSection())
    TestCase.assertEquals(7, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNull(section)
    for (i in 1..7) {
      TestCase.assertEquals(i, course.getLesson("lesson$i")!!.index)
    }
  }

  fun `test unwrap lessons tree structure`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson("lesson2")
        lesson("lesson3")
      }
      lesson("lesson4")
    }
    val section2 = findFile("section2")
    testAction(dataContext(arrayOf(section2)), CCRemoveSection())
    val expectedFileTree = fileTree {
      dir("lesson1")
      dir("lesson2")
      dir("lesson3")
      dir("lesson4")
    }
    expectedFileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test course has the same named lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson("lesson1")
        lesson("lesson2")
      }
      lesson()
    }
    val section2 = findFile("section2")
    try {
      testAction(dataContext(arrayOf(section2)), CCRemoveSection())
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
