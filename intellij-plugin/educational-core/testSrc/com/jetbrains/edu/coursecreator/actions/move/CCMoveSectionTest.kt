package com.jetbrains.edu.coursecreator.actions.move

import com.jetbrains.edu.learning.actions.move.MoveTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class CCMoveSectionTest : MoveTestBase() {

  @Test
  fun `test move section before lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson("lesson2")
      }
    }
    val sourceDir = findPsiDirectory("section2")
    val targetDir = findPsiDirectory("lesson1")

    doMoveAction(course, sourceDir, targetDir, delta = 0)

    assertEquals(2, course.items.size)
    assertEquals(1, course.getSection("section2")!!.index)
    assertEquals(2, course.getLesson("lesson1")!!.index)
  }

  @Test
  fun `test move section after lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson("lesson2")
      }
      lesson()
      lesson()
    }
    val sourceDir = findPsiDirectory("section1")
    val targetDir = findPsiDirectory("lesson1")

    doMoveAction(course, sourceDir, targetDir, delta = 1)

    assertEquals(3, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getSection("section1")!!.index)
    assertEquals(3, course.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test move section before section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section()
      section()
    }
    val sourceDir = findPsiDirectory("section3")
    val targetDir = findPsiDirectory("section2")

    doMoveAction(course, sourceDir, targetDir, delta = 0)

    assertEquals(3, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getSection("section3")!!.index)
    assertEquals(3, course.getSection("section2")!!.index)
  }

  @Test
  fun `test move section after section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section()
      section()
      lesson()
    }
    val sourceDir = findPsiDirectory("section2")
    val targetDir = findPsiDirectory("section3")

    doMoveAction(course, sourceDir, targetDir, delta = 1)

    assertEquals(4, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getSection("section3")!!.index)
    assertEquals(3, course.getSection("section2")!!.index)
    assertEquals(4, course.getLesson("lesson2")!!.index)
  }
}
