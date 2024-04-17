package com.jetbrains.edu.learning.stepik.hyperskill.twitter

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourseWithFiles
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.twitter.TwitterSettings
import com.jetbrains.edu.learning.twitter.ui.TwitterDialogUI
import com.jetbrains.edu.learning.twitter.ui.withMockTwitterDialogUI
import com.jetbrains.edu.learning.ui.getUICheckLabel
import org.junit.Test

class HyperskillTwittingTest : EduActionTestCase() {

  override fun setUp() {
    super.setUp()
    TwitterSettings.getInstance().askToTweet = true
  }

  override fun tearDown() {
    try {
      TwitterSettings.getInstance().askToTweet = false
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test show dialog after last project task solution`() =
    doTest(lessonName = "Project", taskName = "Task2", shouldDialogBeShown = true) { course, task ->
      (course as HyperskillCourse).getProjectLesson()?.visitTasks {
        if (it != task) {
          it.status = CheckStatus.Solved
        }
      }
    }

  @Test
  fun `test do not show dialog in course creator mode`() =
    doTest(lessonName = "Project", taskName = "Task2", createCourse = { createHyperskillCourse(CourseMode.EDUCATOR) }) { course, task ->
      (course as HyperskillCourse).getProjectLesson()?.visitTasks {
        if (it != task) {
          it.status = CheckStatus.Solved
        }
      }
    }

  @Test
  fun `test do not show dialog if not all tasks is solved`() = doTest(lessonName = "Project", taskName = "Task1")

  @Test
  fun `test do not show dialog if not project task solved`() = doTest(HYPERSKILL_TOPICS, TOPIC_NAME, "CodeTask") { course, _ ->
    course.getLesson(HYPERSKILL_TOPICS, TOPIC_NAME)?.visitTasks {
      it.status = CheckStatus.Solved
    }
  }

  @Test
  fun `test do not show dialog if task solved again`() = doTest(lessonName = "Project", taskName = "Task2") { course, _ ->
    (course as HyperskillCourse).getProjectLesson()?.visitTasks {
      it.status = CheckStatus.Solved
    }
  }

  @Test
  fun `test do not show dialog for edu course`() =
    doTest(lessonName = "Project", taskName = "Task2", createCourse = ::createEduCourse) { course, task ->
      course.visitTasks {
        if (it != task) {
          it.status = CheckStatus.Solved
        }
      }
    }

  private fun doTest(
    sectionName: String? = null,
    lessonName: String,
    taskName: String,
    shouldDialogBeShown: Boolean = false,
    createCourse: () -> Course = { createHyperskillCourse() },
    preparationAction: (Course, Task) -> Unit = { _, _ -> }
  ) {
    val course = createCourse()
    val firstTask = course.lessons.first().taskList.first()

    val task = course.getLesson(sectionName, lessonName)?.getTask(taskName)
               ?: error("Can't find `$taskName` in `$lessonName`" + if (sectionName != null) ", section: $sectionName" else "")
    NavigationUtils.navigateToTask(project, task, firstTask, false)

    preparationAction(course, task)

    val isDialogShown = launchCheckAction(task)
    assertEquals(shouldDialogBeShown, isDialogShown)
  }

  private fun createHyperskillCourse(courseMode: CourseMode = CourseMode.STUDENT): HyperskillCourse {
    return hyperskillCourseWithFiles(courseMode = courseMode) {
      frameworkLesson("Project") {
        eduTask("Task1") {
          taskFile("task.txt")
        }
        eduTask("Task2") {
          taskFile("task.txt")
        }
      }

      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          codeTask("CodeTask") {
            taskFile("task.txt")
          }
        }
      }
    }
  }

  private fun createEduCourse(): EduCourse {
    return courseWithFiles {
      frameworkLesson("Project") {
        eduTask("Task1") {
          taskFile("task.txt")
        }
        eduTask("Task2") {
          taskFile("task.txt")
        }
      }

    } as EduCourse
  }


  private fun launchCheckAction(task: Task): Boolean {
    var isDialogShown = false
    withMockTwitterDialogUI(object : TwitterDialogUI {
      override val message: String get() = "Twitter message"

      override fun showAndGet(): Boolean {
        isDialogShown = true
        return false
      }
    }) {
      val taskFile = task.taskFiles.values.first()
      val virtualFile = taskFile.getVirtualFile(project)
                        ?: error("Can't find virtual file for `${taskFile.name}` task file in `${task.name}` task")
      FileEditorManager.getInstance(project).openFile(virtualFile, true)
      testAction(CheckAction(task.getUICheckLabel()))
    }
    return isDialogShown
  }

  companion object {
    private const val TOPIC_NAME = "topicName"
  }
}
