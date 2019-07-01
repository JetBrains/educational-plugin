package com.jetbrains.edu.coursecreator.courseView

import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseView.CourseViewTestBase

class CCCourseViewTest : CourseViewTestBase() {

  @Suppress("DEPRECATION")
  fun `test lesson with custom name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
    }

    val lesson = course.lessons.first()

    doTest(lesson, "-Project\n" +
                   " -CCCourseNode Test Course (Course Creation)\n" +
                   "  CCLessonNode custom name (lesson1)\n")
  }

  @Suppress("DEPRECATION")
  fun `test section with custom name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section()
    }

    val section = course.sections.first()

    doTest(section, "-Project\n" +
                    " -CCCourseNode Test Course (Course Creation)\n" +
                    "  CCSectionNode custom name (section1)\n")
  }

  @Suppress("DEPRECATION")
  fun `test task with custom name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
        eduTask()
      }
    }

    val task = course.lessons.first().taskList.first()

    doTest(task, "-Project\n" +
                 " -CCCourseNode Test Course (Course Creation)\n" +
                 "  -CCLessonNode lesson1\n" +
                 "   +CCTaskNode custom name (task1)\n" +
                 "   +CCTaskNode task2")
  }

  private fun doTest(item: StudyItem, structure: String) {
    item.customPresentableName = "custom name"
    val pane = createPane()

    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }
}