package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.configurationStore.saveSettings
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.CourseReopeningTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.progress.ModalTaskOwner
import com.jetbrains.edu.learning.progress.runWithModalProgressBlocking

class CCFrameworkLessonManagerCourseReopeningTest : CourseReopeningTestBase<EmptyProjectSettings>() {
  override val defaultSettings = EmptyProjectSettings

  fun `test records stay the same after course reopening`() {
    val initialCourse = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson") {
        eduTask("task") {
          taskFile("Task.kt", "fun foo() = 12")
        }
      }
    }

    openStudentProjectThenReopenStudentProject(initialCourse, ::firstOpen, ::secondOpen)
  }

  private fun firstOpen(project: Project) {
    val recordState = CCFrameworkLessonManager.getInstance(project)
    val course = project.course
    val task = course?.findTask("lesson", "task") ?: error("Can't find task")

    with(recordState) {
      val oldRecord = getRecord(task)
      assertEquals(-1, oldRecord)
      updateRecord(task, 12)
      assertEquals(12, getRecord(task))
    }

    // project settings are not saved in tests by default, here it saves them manually
    runWithModalProgressBlocking(ModalTaskOwner.project(project), "") {
      saveSettings(project, true)
    }
  }

  private fun secondOpen(project: Project) {
    val recordState = CCFrameworkLessonManager.getInstance(project)
    val course = project.course
    val task = course?.findTask("lesson", "task") ?: error("Can't find task")

    assertEquals(12, recordState.getRecord(task))
  }
}