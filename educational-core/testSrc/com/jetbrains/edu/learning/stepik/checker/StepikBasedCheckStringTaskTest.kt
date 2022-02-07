package com.jetbrains.edu.learning.stepik.checker

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.Step
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.testAction

abstract class StepikBasedCheckStringTaskTest : StepikBasedCheckAnswerTaskTest() {
  fun `test string task correct`() {
    configureResponses(true)

    CheckActionListener.reset()
    CheckActionListener.expectedMessage { "<html>Succeed solution</html>" }
    val task = myCourse.allTasks[0] as StringTask
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction.ACTION_ID)
    assertEquals("answer", task.getInputAnswer(project))
  }

  fun `test string task incorrect`() {
    configureResponses(false)

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Wrong solution" }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[0])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test string task input is empty`() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { EduCoreBundle.message("hyperskill.string.task.empty.text") }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[1])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test creating placeholder`() {
    val task = myCourse.allTasks[0] as StringTask
    val lesson = task.lesson
    val stepSource = StepSource().apply {
      block = Step().apply {
        name = "string"
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
}