package com.jetbrains.edu.assistant.validation.actions.next.step.hint.code.sequential

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.assistant.validation.actions.next.step.hint.BaseAssistantInfoStorage
import com.jetbrains.edu.assistant.validation.actions.next.step.hint.code.CodeValidationAction
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.check.EduAssistantValidationCheckListener
import com.jetbrains.edu.learning.eduAssistant.core.AssistantResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

abstract class SequentialCodeValidationAction<T> : CodeValidationAction<T>() {

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<T> {
    return getCodeFromTaskFiles(task, lesson).map {
      runBlockingCancellable {
        buildCodeHintRecords(task, it, lesson)
      }
    }.flatten()
  }

  private fun Task.resetStatus() {
    status = CheckStatus.Unchecked
  }

  private suspend fun waitForProjectConfiguration(project: Project) {
    while (DumbService.isDumb(project)) {
      delay(50)
    }
  }

  abstract fun getRecord(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AssistantResponse?,
    userCode: String,
    currentUserCode: String
  ) : T

  abstract fun getRecordWhenError(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AssistantResponse?,
    userCode: String,
    e: Throwable
  ) : T

  abstract fun needToBreak(task: EduTask): Boolean

  private suspend fun buildCodeHintRecords(
    task: EduTask,
    userCode: String,
    lesson: Lesson,
  ): List<T> {
    val baseAssistantInfoStorage = BaseAssistantInfoStorage(task)
    waitForProjectConfiguration(baseAssistantInfoStorage.project)

    // To avoid saving old task states
    task.resetStatus()
    runCheckAction(baseAssistantInfoStorage)

    val records = mutableListOf<T>()
    var currentUserCode = userCode

    for (hintIndex in 1..MAX_HINTS) {
      var response: AssistantResponse? = null
      try {
        runBlockingCancellable {
          withBackgroundProgress(baseAssistantInfoStorage.project, GETTING_HINT_MESSAGE, false) {
            response = baseAssistantInfoStorage.assistant.getHint(
              baseAssistantInfoStorage.taskProcessor,
              baseAssistantInfoStorage.eduState,
              currentUserCode
            )
          }
        }

        currentUserCode = response?.codeHint ?: error("Code hint is empty")

        lesson.replaceContent(task, currentUserCode, baseAssistantInfoStorage.eduState, baseAssistantInfoStorage.project)

        runCheckAction(baseAssistantInfoStorage)

        records.add(getRecord(task, hintIndex, baseAssistantInfoStorage, response, userCode, currentUserCode))

        if (needToBreak(task)) {
          break
        }
      }
      catch (e: Throwable) {
        records.add(getRecordWhenError(task, hintIndex, baseAssistantInfoStorage, response, userCode, e))
      }
    }

    lesson.replaceContent(task, currentUserCode, baseAssistantInfoStorage.eduState, baseAssistantInfoStorage.project)
    return records
  }

  private suspend fun runCheckAction(baseAssistantInfoStorage: BaseAssistantInfoStorage) {
    val checkListener = CheckListener.EP_NAME.findExtension(EduAssistantValidationCheckListener::class.java)
                        ?: error("Check listener not found")
    checkListener.clear()

    withContext(Dispatchers.EDT) {
      val dataContext = SimpleDataContext.getProjectContext(baseAssistantInfoStorage.project)
      ActionUtil.invokeAction(CheckAction(), dataContext, "", null, null)
    }

    checkListener.wait()
  }

  companion object {
    private const val GETTING_HINT_MESSAGE = "Getting Hint"
    private const val MAX_HINTS = 10
  }
}
