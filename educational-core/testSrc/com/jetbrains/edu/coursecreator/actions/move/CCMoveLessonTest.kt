package com.jetbrains.edu.coursecreator.actions.move

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.handlers.CCLessonMoveHandlerDelegate
import com.jetbrains.edu.learning.courseFormat.StudyItem
import junit.framework.TestCase

class CCMoveLessonTest : CCActionTestCase() {

  fun `test move lesson to section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson("lesson2")
      }
    }
    val sourceVFile = findFile("lesson1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerDelegate()
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(1, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertEquals(2, section!!.items.size)
    TestCase.assertEquals(1, section.getLesson("lesson2")!!.index)
    TestCase.assertEquals(2, section.getLesson("lesson1")!!.index)
  }

  fun `test move lesson before lesson in course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson()
      }
      lesson()
    }
    val sourceVFile = findFile("lesson1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getSection("section2")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)
  }

  fun `test move lesson after lesson in course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson()
      section {
        lesson()
      }
    }
    val sourceVFile = findFile("lesson1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerTest(1)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(3, course.getSection("section3")!!.index)
  }

  fun `test move lesson from section to course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson()
      section {
        lesson("lesson3")
      }
    }
    val sourceVFile = findFile("section3/lesson3")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = LightPlatformTestCase.getSourceRoot()
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile!!)

    val handler = CCLessonMoveHandlerDelegate()
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(4, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, course.getSection("section3")!!.index)
    TestCase.assertEquals(4, course.getLesson("lesson3")!!.index)
  }

  fun `test move lesson in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson()
        lesson()
        lesson()
      }
    }
    val sourceVFile = findFile("section1/lesson3")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section1/lesson1")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerTest(1)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val section = course.getSection("section1")
    TestCase.assertEquals(3, section!!.items.size)
    TestCase.assertEquals(1, section.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section.getLesson("lesson3")!!.index)
    TestCase.assertEquals(3, section.getLesson("lesson2")!!.index)
  }

  fun `test move lesson from section to section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE){
      section {
        lesson()
        lesson()
        lesson()
      }
      section {
        lesson("lesson4")
        lesson("lesson5")
        lesson("lesson6")
      }
    }
    val sourceVFile = findFile("section1/lesson2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerDelegate()
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val section1 = course.getSection("section1")
    val section2 = course.getSection("section2")

    TestCase.assertEquals(2, section1!!.items.size)
    TestCase.assertEquals(1, section1.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section1.getLesson("lesson3")!!.index)

    TestCase.assertEquals(4, section2!!.items.size)
  }

  fun `test move lesson from section to lesson in another section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE){
      section {
        lesson()
        lesson()
        lesson()
      }
      section {
        lesson("lesson4")
        lesson("lesson5")
      }
    }
    val sourceVFile = findFile("section1/lesson2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section2/lesson5")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    val section1 = course.getSection("section1")
    val section2 = course.getSection("section2")

    TestCase.assertEquals(2, section1!!.items.size)
    TestCase.assertEquals(1, section1.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, section1.getLesson("lesson3")!!.index)

    TestCase.assertEquals(3, section2!!.items.size)
    TestCase.assertEquals(1, section2.getLesson("lesson4")!!.index)
    TestCase.assertEquals(2, section2.getLesson("lesson2")!!.index)
    TestCase.assertEquals(3, section2.getLesson("lesson5")!!.index)
  }

  internal inner class CCLessonMoveHandlerTest(private val myDelta: Int) : CCLessonMoveHandlerDelegate() {
    override fun getDelta(project: Project, targetItem: StudyItem): Int {
      return myDelta
    }
  }
}
