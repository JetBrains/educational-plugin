package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.hyperskill.MockWebSocketState
import com.jetbrains.edu.learning.stepik.hyperskill.confirmConnection
import com.jetbrains.edu.learning.stepik.hyperskill.confirmSubscription
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.webSocketConfiguration
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.util.*

class HyperskillCheckRemoteEduTaskTest : HyperskillCheckActionTestBase() {

  override fun setUp() {
    super.setUp()
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        remoteEduTask("task1", stepId = 4) {
          taskFile("Task.txt", "fun foo() {}")
          taskFile("tests/Test.txt", "fun foo() {}")
        }
      }
    }
  }

  @Test
  fun `test successful check via web socket`() {
    configureResponses()

    var state: MockWebSocketState = MockWebSocketState.INITIAL

    mockConnector.withWebSocketListener(object : WebSocketListener() {
      override fun onMessage(webSocket: WebSocket, text: String) {
        when (state) {
          MockWebSocketState.INITIAL -> {
            webSocket.confirmConnection()
            state = MockWebSocketState.CONNECTION_CONFIRMED
          }

          MockWebSocketState.CONNECTION_CONFIRMED -> {
            webSocket.confirmSubscription()
            webSocket.send(submissionResult)
          }
        }
      }
    })

    val task = findTask(0, 0)
    checkCheckAction(task, CheckStatus.Solved, "<html>Succeed solution</html>")
  }

  private fun configureResponses() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/ws" -> webSocketConfiguration
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          else -> if (path.startsWith("/api/submissions/")) submissionWithSucceedStatus else "{}"
        }
      )
    }
  }

  @Language("JSON")
  private val submissionWithSucceedStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "11",
          "id": "11",
          "status": "succeed",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1
        }
      ]
    }
  """

  @Language("JSON")
  private val submissionResult: String = """
    {
      "result": {
        "channel": "submission#6242591-0",
        "data": {
          "data": $submissionWithSucceedStatus
        }
      }
    }    
   """

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
          "id": 7565799,
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
          "attempt": "7565799",
          "id": "7565000",
          "status": "evaluation",
          "step": 4368,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """
}