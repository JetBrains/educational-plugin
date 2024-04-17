package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.MockWebSocketState
import com.jetbrains.edu.learning.stepik.hyperskill.confirmConnection
import com.jetbrains.edu.learning.stepik.hyperskill.confirmSubscription
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.webSocketConfiguration
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.intellij.lang.annotations.Language
import org.junit.Test

class HyperskillCheckCodeTaskTest : HyperskillCheckActionTestBase() {

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        codeTask("task1", stepId = 4) {
          taskFile("Task.txt", "fun foo() {}")
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
    checkCheckAction(task, CheckStatus.Failed, "Failed")
  }

  @Test
  fun `test submission made, result not received via web socket`() {
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
            Thread.sleep(500)
            webSocket.cancel() // close violently otherwise need to wait until full timeout exceeded
          }
        }
      }
    })

    val task = findTask(0, 0)
    checkCheckAction(task, CheckStatus.Failed, "Failed")
  }

  @Test
  fun `test no submission made, result received via REST API`() {
    configureResponses()

    mockConnector.withWebSocketListener(object : WebSocketListener() {
      override fun onMessage(webSocket: WebSocket, text: String) {
        webSocket.cancel() // close violently
      }
    })

    val task = findTask(0, 0)
    checkCheckAction(task, CheckStatus.Failed, "Failed")
  }

  @Test
  fun `test failed to get submission status via API`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/ws" -> webSocketConfiguration
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          else -> if (path.startsWith("/api/submissions/")) submissionWithEvaluationStatus else "{}"
        }
      )
    }

    mockConnector.withWebSocketListener(object : WebSocketListener() {
      override fun onMessage(webSocket: WebSocket, text: String) {
        webSocket.cancel() // close violently
      }
    })

    val task = findTask(0, 0)
    checkCheckAction(task, CheckStatus.Unchecked)
  }

  private fun configureResponses() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/ws" -> webSocketConfiguration
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          else -> if (path.startsWith("/api/submissions/")) submissionWithWrongStatus else "{}"
        }
      )
    }
  }

  @Language("JSON")
  private val submissionWithWrongStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "id": 7565000,
          "attempt": 7565799,
          "feedback": {
            "message": "Failed"
          },
          "hint": "Failed",
          "reply": {
            "language": "kotlin",
            "code": "fun main() {\n    TODO(\"Remove this line and write your solution here\")\n}\n"
          },
          "status": "wrong",
          "step": 4368,
          "time": "2020-04-29T13:39:55Z"
        }
      ]
    }
  """

  @Language("JSON")
  private val submissionWithEvaluationStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "id": 7565000,
          "attempt": 7565799,
          "feedback": {
            "message": "Failed"
          },
          "hint": "Failed",
          "reply": {
            "language": "kotlin",
            "code": "fun main() {\n    TODO(\"Remove this line and write your solution here\")\n}\n"
          },
          "status": "evaluation",
          "step": 4368,
          "time": "2020-04-29T13:39:55Z"
        }
      ]
    }
  """

  @Language("JSON")
  private val submissionResult: String = """
    {
      "push": {
        "channel": "submission#6242591-0",
        "pub": {
          "data": $submissionWithWrongStatus
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