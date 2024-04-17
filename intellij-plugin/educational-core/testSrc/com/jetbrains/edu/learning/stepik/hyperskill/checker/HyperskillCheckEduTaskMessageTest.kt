package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.pathWithoutPrams
import org.intellij.lang.annotations.Language
import org.junit.Test

class HyperskillCheckEduTaskMessageTest : HyperskillCheckActionTestBase() {

  override fun createCourse() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
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
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1))
    course.hyperskillProject = HyperskillProject()
  }

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  @Test
  fun `test solve all edu tasks in topic`() {
    val course = getCourse() as HyperskillCourse
    checkCheckAction(
      course.getProjectLesson()!!.taskList[0],
      CheckStatus.Solved,
      EduCoreBundle.message("hyperskill.next.project", HYPERSKILL_PROJECTS_URL)
    )

    val topic = course.getTopicsSection()!!.lessons[0]
    checkCheckAction(topic.taskList[0], CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
    checkCheckAction(topic.taskList[1], CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
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