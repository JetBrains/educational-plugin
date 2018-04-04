package com.jetbrains.edu.coursecreator.actions.delete

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCVirtualFileListener
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import junit.framework.TestCase

class CCDeleteListenerTest : CCActionTestCase() {

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

  fun `test delete section`() {
    val course = courseWithFiles {
      lesson {}
      section {}
      lesson {}
    }
    course.courseMode = CCUtils.COURSE_MODE
    val sectionVFile = LightPlatformTestCase.getSourceRoot().findChild("section2")
    runWriteAction {
      sectionVFile!!.delete(this)
    }

    TestCase.assertEquals(2, course.items.size)
    TestCase.assertNull(course.getSection("section2"))
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
  }

  fun `test delete lesson`() {
    val course = courseWithFiles {
      lesson {}
      section {}
      lesson {}
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lesson1 = LightPlatformTestCase.getSourceRoot().findChild("lesson1")
    runWriteAction {
      lesson1!!.delete(this)
    }

    TestCase.assertEquals(2, course.items.size)
    TestCase.assertNull(course.getLesson("lesson1"))
    TestCase.assertEquals(1, course.getSection("section2")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
  }


  fun `test delete lesson from section`() {
    val course = courseWithFiles {
      lesson {}
      section {
        lesson {}
        lesson {}
      }
      lesson {}
    }
    course.courseMode = CCUtils.COURSE_MODE
    val lesson1 = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "section2", "lesson1")
    runWriteAction {
      lesson1!!.delete(this)
    }

    val section = course.getSection("section2")!!
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)

    TestCase.assertNull(section.getLesson("lesson1"))
    TestCase.assertEquals(1, section.items.size)
    TestCase.assertEquals(1, section.getLesson("lesson2")!!.index)
  }

  fun `test delete task`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("tmp.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val task1 = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "lesson1", "task1")
    runWriteAction {
      task1!!.delete(this)
    }

    val lesson = course.getLesson("lesson1")
    TestCase.assertNull(lesson!!.getTask("task1"))
  }

  fun `test delete task file`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("tmp.txt")
        }
      }
    }
    course.courseMode = CCUtils.COURSE_MODE
    val taskFile = VfsUtil.findRelativeFile(LightPlatformTestCase.getSourceRoot(), "lesson1", "task1", "tmp.txt")
    runWriteAction {
      taskFile!!.delete(this)
    }

    val lesson = course.getLesson("lesson1")
    TestCase.assertTrue(lesson!!.getTask("task1").taskFiles.isEmpty())
  }
}
