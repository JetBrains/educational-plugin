package com.jetbrains.edu.coursecreator.actions.move

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.handlers.CCSectionMoveHandlerDelegate
import com.jetbrains.edu.learning.courseFormat.StudyItem
import junit.framework.TestCase

class CCMoveSectionTest : CCActionTestCase() {

  fun `test move section before lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson("lesson2")
      }
    }
    val sourceVFile = findFile("section2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson1")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCSectionMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(2, course.items.size)
    TestCase.assertEquals(1, course.getSection("section2")!!.index)
    TestCase.assertEquals(2, course.getLesson("lesson1")!!.index)
  }

  fun `test move section after lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson("lesson2")
      }
      lesson()
      lesson()
    }
    val sourceVFile = findFile("section1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson1")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCSectionMoveHandlerTest(1)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section1")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)
  }

  fun `test move section before section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section()
      section()
    }
    val sourceVFile = findFile("section3")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCSectionMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(3, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section3")!!.index)
    TestCase.assertEquals(3, course.getSection("section2")!!.index)
  }

  fun `test move section after section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section()
      section()
      lesson()
    }
    val sourceVFile = findFile("section2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section3")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCSectionMoveHandlerTest(1)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})
    TestCase.assertEquals(4, course.items.size)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section3")!!.index)
    TestCase.assertEquals(3, course.getSection("section2")!!.index)
    TestCase.assertEquals(4, course.getLesson("lesson2")!!.index)
  }

  internal inner class CCSectionMoveHandlerTest(private val myDelta: Int) : CCSectionMoveHandlerDelegate() {
    override fun getDelta(project: Project, targetItem: StudyItem): Int {
      return myDelta
    }
  }
}
