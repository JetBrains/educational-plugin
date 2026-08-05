package com.jetbrains.edu.learning.courseView

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findLesson
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.projectView.CourseViewVisibleItems
import org.junit.Test

class CourseViewSubtreeTest : CourseViewTestBase() {

  @Test
  fun `test whole course is shown while subtree mode is disabled`() {
    // given
    createCourseWithIds()

    // then
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/5
        -SectionNode section1
         -LessonNode lesson1
          -TaskNode task1
           Task.txt
          -TaskNode task2
           Task.txt
         -LessonNode lesson2
          -TaskNode task1
           Task.txt
        -SectionNode section2
         -LessonNode lesson1
          -TaskNode task1
           Task.txt
        -LessonNode lesson1
         -TaskNode task1
          Task.txt
        README.md
    """.trimIndent())
  }

  @Test
  fun `test only course node is shown until something is revealed`() {
    // given
    createCourseWithIds()

    // when
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // then
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/5
        README.md
    """.trimIndent())
  }

  @Test
  fun `test only revealed task is shown inside its lesson and section`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(course.findTask("lesson2", "task1", "section1"))

    // then
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/5
        -SectionNode section1
         -LessonNode lesson2
          -TaskNode task1
           Task.txt
        README.md
    """.trimIndent())
  }

  @Test
  fun `test all tasks of revealed lesson are shown`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(course.findLesson("lesson1", "section1"))

    // then
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/5
        -SectionNode section1
         -LessonNode lesson1
          -TaskNode task1
           Task.txt
          -TaskNode task2
           Task.txt
        README.md
    """.trimIndent())
  }

  @Test
  fun `test top level lesson is shown when its task is revealed`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(course.findTask("lesson1", "task1"))

    // then
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/5
        -LessonNode lesson1
         -TaskNode task1
          Task.txt
        README.md
    """.trimIndent())
  }

  @Test
  fun `test framework lesson is shown when its task is revealed`() {
    // given
    val course = courseWithFiles {
      frameworkLesson("lesson1", id = 1) {
        eduTask("task1", stepId = 11) { taskFile("Task.txt") }
        eduTask("task2", stepId = 12) { taskFile("Task.txt") }
      }
      lesson("lesson2", id = 2) {
        eduTask("task1", stepId = 21) { taskFile("Task.txt") }
      }
    }
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(course.findTask("lesson1", "task2"))

    // then
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/2
        -FrameworkLessonNode lesson1
         Task.txt
    """.trimIndent())
  }

  @Test
  fun `test whole course is shown again when subtree mode is disabled`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(course.findTask("lesson2", "task1", "section1"))

    // when
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = false

    // then
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/5
        -SectionNode section1
         -LessonNode lesson1
          -TaskNode task1
           Task.txt
          -TaskNode task2
           Task.txt
         -LessonNode lesson2
          -TaskNode task1
           Task.txt
        -SectionNode section2
         -LessonNode lesson1
          -TaskNode task1
           Task.txt
        -LessonNode lesson1
         -TaskNode task1
          Task.txt
        README.md
    """.trimIndent())
  }

  @Test
  fun `test whole course is shown in course creator mode`() {
    // given
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 11) { taskFile("Task.txt") }
        eduTask("task2", stepId = 12) { taskFile("Task.txt") }
      }
    }

    // when
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // then
    assertCourseView("""
      -Project
       -CCCourseNode Test Course (Course Creation)
        -CCLessonNode lesson1
         -CCTaskNode task1
          CCStudentInvisibleFileNode task.md
          Task.txt
         -CCTaskNode task2
          CCStudentInvisibleFileNode task.md
          Task.txt
    """.trimIndent())
  }

  private fun createCourseWithIds(): Course = courseWithFiles {
    section("section1", id = 1) {
      lesson("lesson1", id = 11) {
        eduTask("task1", stepId = 111) { taskFile("Task.txt") }
        eduTask("task2", stepId = 112) { taskFile("Task.txt") }
      }
      lesson("lesson2", id = 12) {
        eduTask("task1", stepId = 121) { taskFile("Task.txt") }
      }
    }
    section("section2", id = 2) {
      lesson("lesson1", id = 21) {
        eduTask("task1", stepId = 211) { taskFile("Task.txt") }
      }
    }
    lesson("lesson1", id = 31) {
      eduTask("task1", stepId = 311) { taskFile("Task.txt") }
    }
    additionalFile("README.md", "Course description") {
      withVisibility(true)
    }
  }
}
