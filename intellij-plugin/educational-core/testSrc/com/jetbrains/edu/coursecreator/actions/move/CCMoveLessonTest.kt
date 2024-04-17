package com.jetbrains.edu.coursecreator.actions.move

import com.jetbrains.edu.learning.actions.move.MoveTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class CCMoveLessonTest : MoveTestBase() {

  @Test
  fun `test move lesson to section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson("lesson2")
      }
    }
    val sourceDir = findPsiDirectory("lesson1")
    val targetDir = findPsiDirectory("section2")

    doMoveAction(course, sourceDir, targetDir)

    assertEquals(1, course.items.size)
    val section = course.getSection("section2")!!
    assertEquals(2, section.items.size)
    assertEquals(1, section.getLesson("lesson2")!!.index)
    assertEquals(2, section.getLesson("lesson1")!!.index)
  }

  @Test
  fun `test move lesson before lesson in course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson()
      }
      lesson()
    }
    val sourceDir = findPsiDirectory("lesson1")
    val targetDir = findPsiDirectory("lesson2")

    doMoveAction(course, sourceDir, targetDir, delta = 0)

    assertEquals(3, course.items.size)
    assertEquals(1, course.getSection("section2")!!.index)
    assertEquals(2, course.getLesson("lesson1")!!.index)
    assertEquals(3, course.getLesson("lesson2")!!.index)
  }


  @Test
  fun `test move lesson after lesson in course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      lesson()
      section {
        lesson()
      }
    }
    val sourceDir = findPsiDirectory("lesson1")
    val targetDir = findPsiDirectory("lesson2")

    doMoveAction(course, sourceDir, targetDir, delta = 1)

    assertEquals(3, course.items.size)
    assertEquals(1, course.getLesson("lesson2")!!.index)
    assertEquals(2, course.getLesson("lesson1")!!.index)
    assertEquals(3, course.getSection("section3")!!.index)
  }

  @Test
  fun `test move lesson from section to course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      lesson()
      section {
        lesson("lesson3")
      }
    }
    val sourceDir = findPsiDirectory("section3/lesson3")
    val targetDir = findPsiDirectory(".")

    doMoveAction(course, sourceDir, targetDir)

    assertEquals(4, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getLesson("lesson2")!!.index)
    assertEquals(3, course.getSection("section3")!!.index)
    assertEquals(4, course.getLesson("lesson3")!!.index)
  }

  @Test
  fun `test move lesson in section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson()
        lesson()
        lesson()
      }
    }
    val sourceDir = findPsiDirectory("section1/lesson3")
    val targetDir = findPsiDirectory("section1/lesson1")

    doMoveAction(course, sourceDir, targetDir, delta = 1)

    val section = course.getSection("section1")!!
    assertEquals(3, section.items.size)
    assertEquals(1, section.getLesson("lesson1")!!.index)
    assertEquals(2, section.getLesson("lesson3")!!.index)
    assertEquals(3, section.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test move lesson from section to section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val sourceDir = findPsiDirectory("section1/lesson2")
    val targetDir = findPsiDirectory("section2")

    doMoveAction(course, sourceDir, targetDir)

    val section1 = course.getSection("section1")!!
    val section2 = course.getSection("section2")!!

    assertEquals(2, section1.items.size)
    assertEquals(1, section1.getLesson("lesson1")!!.index)
    assertEquals(2, section1.getLesson("lesson3")!!.index)

    assertEquals(4, section2.items.size)
  }

  // EDU-2467
  @Test
  fun `test move from section with the same name to section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section("section1") {
        lesson("section1")
      }
      section {
        lesson("lesson4")
      }
    }

    val sourceDir = findPsiDirectory("section1/section1")
    val targetDir = findPsiDirectory("section2")

    doMoveAction(course, sourceDir, targetDir, delta = 1)

    val section1 = course.getSection("section1")
    val section2 = course.getSection("section2")

    assertEquals(0, section1!!.items.size)
    assertEquals(2, section2!!.items.size)
  }

  @Test
  fun `test move lesson from section to lesson in another section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    val sourceDir = findPsiDirectory("section1/lesson2")
    val targetDir = findPsiDirectory("section2/lesson5")

    doMoveAction(course, sourceDir, targetDir, delta = 0)

    val section1 = course.getSection("section1")!!
    val section2 = course.getSection("section2")!!

    assertEquals(2, section1.items.size)
    assertEquals(1, section1.getLesson("lesson1")!!.index)
    assertEquals(2, section1.getLesson("lesson3")!!.index)

    assertEquals(3, section2.items.size)
    assertEquals(1, section2.getLesson("lesson4")!!.index)
    assertEquals(2, section2.getLesson("lesson2")!!.index)
    assertEquals(3, section2.getLesson("lesson5")!!.index)
  }
}
