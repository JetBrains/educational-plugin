package com.jetbrains.edu.learning.stepik.checker

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.intellij.lang.annotations.Language

abstract class StepikBasedCheckDataTaskTest : EduTestCase() {

  protected fun checkTask(task: Task): List<AssertionError> {
    val project = getCourse().project!!
    val assertions = CheckersTestBase.checkTaskWithProject(task, project)
    assertEmpty(assertions)
    return assertions
  }

  @Language("JSON")
  protected val submissionWithEvaluationStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "$ATTEMPT_ID_OF_SUCCEED_SUBMISSION",
          "id": "$SUBMISSION_ID",
          "status": "evaluation",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  protected val submissionWithSucceedStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "$ATTEMPT_ID_OF_SUCCEED_SUBMISSION",
          "id": "$SUBMISSION_ID",
          "status": "succeed",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  protected val submissionWithFailedStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "$ATTEMPT_ID_OF_FAILED_SUBMISSION",
          "id": "$SUBMISSION_ID",
          "status": "wrong",
          "step": 2,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 123
        }
      ]
    }
  """

  companion object {
    @JvmStatic
    protected val DATA_TASK_1: String = "Data Task 1"

    @JvmStatic
    protected val DATA_TASK_2: String = "Data Task 2"

    @JvmStatic
    protected val ATTEMPT_ID_OF_SUCCEED_SUBMISSION: Int = 101

    @JvmStatic
    protected val ATTEMPT_ID_OF_FAILED_SUBMISSION: Int = 102

    @JvmStatic
    protected val SUBMISSION_ID: Int = 100
  }
}