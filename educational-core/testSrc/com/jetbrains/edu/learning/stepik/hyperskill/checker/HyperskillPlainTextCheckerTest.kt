package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.actions.getCurrentTask
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.markStageAsCompleted
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class HyperskillPlainTextCheckerTest : CheckersTestBase<EmptyProjectSettings>() {

  override fun createCheckerFixture(): EduCheckerFixture<EmptyProjectSettings> = PlaintTextCheckerFixture()

  override fun createCourse(): Course {
    val course = course(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask(stepId = 1) {
          checkResultFile(CheckStatus.Solved)
        }
        eduTask(stepId = 2) {
          checkResultFile(CheckStatus.Solved)
        }
      }
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1), HyperskillStage(2, "", 2))
    course.hyperskillProject = HyperskillProject()
    return course
  }

  fun `test course`() {
    CheckActionListener.expectedMessage { task ->
      when (task.index) {
        1 -> CheckUtils.CONGRATULATIONS
        2 -> EduCoreBundle.message("hyperskill.next.project", HYPERSKILL_PROJECTS_URL)
        else -> null
      }
    }
    TaskDescriptionView.getInstance(project).currentTask = project.getCurrentTask()
    doTest()
  }

  override fun checkTask(task: Task): List<AssertionError> {
    val assertions = super.checkTask(task)
    if (assertions.isEmpty() && task.status == CheckStatus.Solved) {
      markStageAsCompleted(task)
    }
    return assertions
  }
}
