package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseReopeningTestBase
import com.jetbrains.edu.learning.actions.EduActionUtils.waitAndDispatchInvocationEvents
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.copy
import com.jetbrains.edu.learning.courseFormat.copyFileContents
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdaterNew
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestUpdateAfterCourseReopening : CourseReopeningTestBase<EmptyProjectSettings>() {
  override val defaultSettings = EmptyProjectSettings

  @Test
  fun `data persisted after update`() {
    val course = course {
      frameworkLesson("lesson1", isTemplateBased = false) {
        eduTask("task1") {
          taskFile("hello.txt", InMemoryTextualContents("hello.txt step1"))
        }
        eduTask("task2") {
          taskFile("hello.txt", InMemoryTextualContents("hello.txt step2"))
        }
        eduTask("task3") {
          taskFile("hello.txt", InMemoryTextualContents("hello.txt step3"))
        }
      }
    }

    openStudentProjectThenReopenStudentProject(course, { project ->
      updateCourse(project, project.course as EduCourse)
    }, { project ->
      val course = project.course as EduCourse
      for (i in 1..3) {
        assertContentsEqual(
          course.findTask("lesson1", "task$i"),
          "hello.txt",
          InMemoryTextualContents("updated hello.txt step$i")
        )
      }
    })
  }

  private fun updateCourse(project: Project, course: EduCourse) {
    val updater = MarketplaceCourseUpdaterNew(project, course)

    val remoteCourse = course.copy().apply {
      additionalFiles = course.additionalFiles
      copyFileContents(course, this)

      // append "updated" to all "hello.txt" contents
      visitTasks {
        val taskFile = it.taskFiles["hello.txt"]!!
        taskFile.contents = InMemoryTextualContents(
          "updated " + taskFile.contents.textualRepresentation
        )
      }

      init(false)
    }

    val future = ApplicationManager.getApplication().executeOnPooledThread {
      runBlocking {
        updater.update(remoteCourse)
      }
    }
    waitAndDispatchInvocationEvents(future)
  }
}