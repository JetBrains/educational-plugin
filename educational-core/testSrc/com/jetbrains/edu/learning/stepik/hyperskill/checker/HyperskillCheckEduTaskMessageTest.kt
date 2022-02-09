package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import org.intellij.lang.annotations.Language

class HyperskillCheckEduTaskMessageTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun createCourse() {
    (courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask(stepId = 1) {
          checkResultFile(CheckStatus.Solved)
        }
      }
      section("Topics") {
        lesson("Topic name") {
          eduTask("Problem name 1") {
            checkResultFile(CheckStatus.Solved)
          }
          eduTask("Problem name 2") {
            checkResultFile(CheckStatus.Solved)
          }
        }
      }
    } as HyperskillCourse)
      .apply {
        stages = listOf(HyperskillStage(1, "", 1))
        stages = listOf(HyperskillStage(1, "", 1))
      }
  }

  override fun setUp() {
    super.setUp()
    configureResponse()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    logOutFakeHyperskillUser()
    super.tearDown()
  }

  fun `test solve all edu tasks in topic`() {
    CheckActionListener.reset()

    val course = getCourse() as HyperskillCourse
    CheckActionListener.expectedMessage { EduCoreBundle.message("hyperskill.next.project", HYPERSKILL_PROJECTS_URL) }
    CheckersTestBase.checkTaskWithProject(course.getProjectLesson()!!.taskList[0], project).apply { assertEmpty(this) }

    val topic = course.getTopicsSection()!!.lessons[0]
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    CheckersTestBase.checkTaskWithProject(topic.taskList[0], project).apply { assertEmpty(this) }
    CheckersTestBase.checkTaskWithProject(topic.taskList[1], project).apply { assertEmpty(this) }
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