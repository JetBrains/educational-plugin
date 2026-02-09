package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderDependencyMixin
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector

interface StepikBasedConnector {
  val platformName: String

  fun getActiveAttempt(task: Task): Result<Attempt?, String>

  fun getActiveAttemptOrPostNew(task: Task): Result<Attempt, String> {
    val activeAttempt = getActiveAttempt(task)
    if (activeAttempt is Ok) {
      val value = activeAttempt.value
      if (value != null) {
        return Ok(value)
      }
    }
    return postAttempt(task)
  }

  fun postAttempt(task: Task): Result<Attempt, String>

  fun getDataset(attempt: Attempt): Result<String, String>

  fun getSubmission(id: Int): Result<StepikBasedSubmission, String>

  fun getSubmissions(stepId: Int): List<StepikBasedSubmission>

  fun postSubmission(submission: StepikBasedSubmission): Result<StepikBasedSubmission, String>

  fun doRefreshTokens()

  fun <T> withTokenRefreshIfFailed(call: () -> Result<T, String>): Result<T, String> {
    val result = call()
    if (!isUnitTestMode && !ApplicationManager.getApplication().isInternal
        && result is Err && result.error == EduCoreBundle.message("error.network.access.denied")) {
      doRefreshTokens()
      return call()
    }
    return result
  }

  companion object {
    fun Course.getStepikBasedConnector(): StepikBasedConnector {
      return when {
        isStepikRemote -> StepikConnector.getInstance()
        this is HyperskillCourse -> HyperskillConnector.getInstance()
        else -> error("Wrong course type: ${course.itemType}")
      }
    }

    fun Task.getStepikBasedConnector(): StepikBasedConnector = course.getStepikBasedConnector()

    fun createObjectMapper(module: SimpleModule): ObjectMapper {
      val objectMapper = ConnectorUtils.createMapper()
      objectMapper.addMixIn(EduCourse::class.java, StepikEduCourseMixin::class.java)
      objectMapper.addMixIn(StepikLesson::class.java, StepikLessonMixin::class.java)
      objectMapper.addMixIn(EduFile::class.java, StepikEduFileMixin::class.java)
      objectMapper.addMixIn(TaskFile::class.java, StepikTaskFileMixin::class.java)
      objectMapper.addMixIn(Task::class.java, StepikTaskMixin::class.java)
      objectMapper.addMixIn(AnswerPlaceholder::class.java, StepikAnswerPlaceholderMixin::class.java)
      objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
      objectMapper.registerModule(module)
      return objectMapper
    }
  }
}