package com.jetbrains.edu.learning.stepik.hyperskill.twitter

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourseWithFiles
import com.jetbrains.edu.learning.twitter.TwitterSettings
import com.jetbrains.edu.learning.twitter.ui.TwitterDialogUI
import com.jetbrains.edu.learning.twitter.ui.withMockTwitterDialogUI

class HyperskillTwittingTest : EduActionTestCase() {

  override fun setUp() {
    super.setUp()
    TwitterSettings.getInstance().setAskToTweet(true)
  }

  override fun tearDown() {
    TwitterSettings.getInstance().setAskToTweet(false)
    super.tearDown()
  }

  fun `test show dialog after last project task solution`() = doTest("Project", "Task2", true) { course, task ->
    (course as HyperskillCourse).getProjectLesson()?.visitTasks {
      if (it != task) {
        it.status = CheckStatus.Solved
      }
    }
  }

  fun `test do not show dialog in course creator mode`() = doTest(
    "Project",
    "Task2",
    false,
    { createHyperskillCourse(CCUtils.COURSE_MODE) }
  ) { course, task ->
    (course as HyperskillCourse).getProjectLesson()?.visitTasks {
      if (it != task) {
        it.status = CheckStatus.Solved
      }
    }
  }

  fun `test do not show dialog if not all tasks is solved`() = doTest("Project", "Task1", false)

  fun `test do not show dialog if not project task solved`() = doTest(HYPERSKILL_PROBLEMS, "CodeTask", false) { course, _ ->
    (course as HyperskillCourse).getProjectLesson()?.visitTasks {
      it.status = CheckStatus.Solved
    }
  }

  fun `test do not show dialog if task solved again`() = doTest("Project", "Task2", false) { course, _ ->
    (course as HyperskillCourse).getProjectLesson()?.visitTasks {
      it.status = CheckStatus.Solved
    }
  }

  fun `test do not show dialog for edu course`() = doTest("Project", "Task2", false, ::createEduCourse) { course, task ->
    course.visitTasks {
      if (it != task) {
        it.status = CheckStatus.Solved
      }
    }
  }

  private fun doTest(
    lessonName: String,
    taskName: String,
    shouldDialogBeShown: Boolean,
    createCourse: () -> Course = { createHyperskillCourse() },
    preparationAction: (Course, Task) -> Unit = { _, _ -> }
  ) {
    val course = createCourse()
    val firstTask = course.lessons.first().taskList.first()

    val task = course.findTask(lessonName, taskName)
    NavigationUtils.navigateToTask(project, task, firstTask, false)

    preparationAction(course, task)

    val isDialogShown = launchCheckAction(task)
    assertEquals(shouldDialogBeShown, isDialogShown)
  }

  private fun createHyperskillCourse(courseMode: String = EduNames.STUDY): HyperskillCourse {
    return hyperskillCourseWithFiles(courseMode = courseMode) {
      frameworkLesson("Project") {
        eduTask("Task1") {
          taskFile("task.txt")
        }
        eduTask("Task2") {
          taskFile("task.txt")
        }
      }

      lesson(HYPERSKILL_PROBLEMS) {
        eduTask("CodeTask") {
          taskFile("task.txt")
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
      testAction(dataContext(virtualFile), CheckAction())
    }
    return isDialogShown
  }
}
