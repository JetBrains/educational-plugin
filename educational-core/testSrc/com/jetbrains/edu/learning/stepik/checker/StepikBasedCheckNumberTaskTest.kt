package com.jetbrains.edu.learning.stepik.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.Step
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.testAction

abstract class StepikBasedCheckNumberTaskTest : StepikBasedCheckAnswerTaskTest() {
  fun `test number task correct`() {
    configureResponses(true)

    CheckActionListener.reset()
    CheckActionListener.expectedMessage { "<html>Succeed solution</html>" }
    val task = myCourse.allTasks[2] as NumberTask
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction.ACTION_ID)
    assertEquals("12", task.getInputAnswer(project))
  }

  fun `test number task incorrect`() {
    configureResponses(false)

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Wrong solution" }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[2])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test number task validation on number`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { EduCoreBundle.message("hyperskill.number.task.not.number") }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[0])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test number task input is empty`() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { EduCoreBundle.message("hyperskill.string.task.empty.text") }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[1])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test creating placeholder`() {
    val task = myCourse.allTasks[0] as NumberTask
    val lesson = task.lesson
    val stepSource = StepSource().apply {
      block = Step().apply {
        name = "number"
      }
    }

    val createdTask = StepikTaskBuilder(myCourse, lesson, stepSource).createTask(stepSource.block?.name!!) ?: error("")
    assertEquals(1, createdTask.taskFiles.size)
    assertEquals(1, createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.size)
    assertEquals(EduCoreBundle.message("string.task.comment.file"),
                 createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.first()?.placeholderText)
    assertEquals(0, createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.first()?.offset)
    assertEquals(EduCoreBundle.message("string.task.comment.file").length,
                 createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.first()?.endOffset)
  }

  fun `test task with space`() {
    assertNull((myCourse.allTasks[3] as NumberTask).validateAnswer (project))
    assertNull((myCourse.allTasks[4] as NumberTask).validateAnswer (project))
    assertNull((myCourse.allTasks[5] as NumberTask).validateAnswer (project))
  }

  fun `test task with comma and dot`() {
    assertNull((myCourse.allTasks[6] as NumberTask).validateAnswer (project))
    assertNull((myCourse.allTasks[7] as NumberTask).validateAnswer (project))
  }
}