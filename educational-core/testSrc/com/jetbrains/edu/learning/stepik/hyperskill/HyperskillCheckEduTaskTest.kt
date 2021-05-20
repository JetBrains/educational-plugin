package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.checker.PlaintTextCheckerFixture
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.intellij.lang.annotations.Language

class HyperskillCheckEduTaskTest : CheckersTestBase<Unit>() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  override fun createCourse(): Course = course(courseProducer = ::HyperskillCourse) {
    section("Topics") {
      lesson("Topic name") {
        eduTask("Problem name 1") {
          taskFile(PlainTextConfigurator.CHECK_RESULT_FILE) {
            withText(CheckStatus.Solved.toString())
          }
        }
        eduTask("Problem name 2") {
          taskFile(PlainTextConfigurator.CHECK_RESULT_FILE) {
            withText(CheckStatus.Failed.toString())
          }
        }
      }
    }
  } as HyperskillCourse

  override fun setUp() {
    super.setUp()
    configureResponse()
    loginFakeUser()
  }

  fun `test solved edu task`() {
    CheckActionListener.reset()
    checkTask(myCourse.allTasks[0])
  }

  fun `test failed edu task`() {
    CheckActionListener.shouldFail()
    checkTask(myCourse.allTasks[1])
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          else -> error("Wrong path: ${path}")
        }
      )
    }
  }

  @Language("JSON")
  private val attempt = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 7565800,
          "status": "active",
          "step": 4368,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """

  @Language("JSON")
  private val submission = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "7565800",
          "id": "7565003",
          "status": "evaluation",
          "step": 4368,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """
}