package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.configurationStore.saveSettings
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.CourseReopeningTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CCFrameworkLessonManagerCourseReopeningTest : CourseReopeningTestBase<EmptyProjectSettings>() {
  override val defaultSettings = EmptyProjectSettings

  @Test
  fun `test records stay the same after course reopening`() {
    val initialCourse = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson") {
        eduTask("task") {
          taskFile("Task.kt", "fun foo() = 12")
        }
      }
    }

    var record = -1

    val firstOpen: (Project) -> Unit = { project: Project ->
      val recordState = CCFrameworkLessonManager.getInstance(project)
      val course = project.course
      val task = course?.findTask("lesson", "task") ?: error("Can't find task")

      with(recordState) {
        val oldRecord = getRecord(task)
        assertEquals(-1, oldRecord)
        saveCurrentState(task)
        record = getRecord(task)
        assertNotEquals(oldRecord, record)
      }

      // project settings are not saved in tests by default, here it saves them manually
      runWithModalProgressBlocking(ModalTaskOwner.project(project), "") {
        saveSettings(project, true)
      }
    }

    openStudentProjectThenReopenStudentProject(initialCourse, firstOpen) {
      val recordState = CCFrameworkLessonManager.getInstance(project)
      val course = project.course
      val task = course?.findTask("lesson", "task") ?: error("Can't find task")

      assertEquals(record, recordState.getRecord(task))
    }
  }
}