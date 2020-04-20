package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillUserInfo
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.intellij.lang.annotations.Language

class HyperskillCheckCodeTaskTest : EduTestCase() {

  private enum class MockWebSocketState {
    INITIAL, CONNECTION_CONFIRMED
  }

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  private fun loginFakeUser() {
    val fakeToken = TokenInfo().apply { accessToken = "faketoken" }
    HyperskillSettings.INSTANCE.account = HyperskillAccount().apply {
      userInfo = HyperskillUserInfo()
      userInfo.id = 1
      tokenInfo = fakeToken
    }
  }

  fun `test result received via web socket`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      when (request.path) {
        "/api/ws" -> MockResponseFactory.fromString("""{"token": "fakeToken","url": "fakeUrl"}""")
        "/api/attempts" -> MockResponseFactory.fromString(attempt)
        "/api/submissions" -> MockResponseFactory.fromString(submission)
        else -> MockResponseFactory.fromString("{}")
      }
    }

    var state: MockWebSocketState = MockWebSocketState.INITIAL

    mockConnector.withWebSocketListener(object : WebSocketListener() {
      override fun onMessage(webSocket: WebSocket, text: String) {
        when (state) {
          MockWebSocketState.INITIAL -> {
            webSocket.send("Connection confirmed")
            state = MockWebSocketState.CONNECTION_CONFIRMED
          }
          MockWebSocketState.CONNECTION_CONFIRMED -> {
            webSocket.send("Subscription confirmed")
            webSocket.send(submissionResult)
          }
        }
      }
    })

    courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        codeTask("task1", stepId = 4) {
          taskFile("Task.txt", "fun foo() {}")
        }
      }
    } as HyperskillCourse

    loginFakeUser()
    NavigationUtils.navigateToTask(project, findTask(0, 0))

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Failed" }
    launchAction()
  }

  private fun launchAction() {
    val action = CheckAction()
    val e = TestActionEvent(action)
    action.beforeActionPerformedUpdate(e)
    assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
    action.actionPerformed(e)
  }

  @Language("JSON")
  private val submissionResult: String = """
    {
      "result": {
        "channel": "submission#6242591-0",
        "data": {
          "data": {
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