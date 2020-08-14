package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.intellij.lang.annotations.Language

class HyperskillSubmissionsTest : SubmissionsTestBase() {

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    loginFakeUser()
    configureResponses()
  }

  private fun configureResponses() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val path = request.path
      MockResponseFactory.fromString(
        when {
          "/api/ws.*".toRegex().matches(path) -> webSocketConfiguration
          "/api/attempts.*".toRegex().matches(path) -> attempt
          "/api/submissions.*".toRegex().matches(path) -> submissions
          else -> "{}"
        }
      )
    }
  }

  fun `test edu task submissions loaded`() {
    hyperskillCourseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    }

    doTestSubmissionsLoaded(setOf(1), mapOf(1 to 1))
  }

  fun `test edu task several submissions loaded`() {
    hyperskillCourseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    }

    doTestSubmissionsLoaded(setOf(2), mapOf(2 to 2))
  }

  fun `test code problem submissions loaded`() {
    hyperskillCourseWithFiles {
      lesson("Problems") {
        codeTask("task1", stepId = 1) {
          taskFile("Task.txt", "fun foo() {}")
        }
      }
    }

    doTestSubmissionsLoaded(setOf(1), mapOf(1 to 1))
  }

  fun `test submission added after edu task check`() {
    hyperskillCourseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 3) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    }
    doTestSubmissionAddedAfterTaskCheck(3, EduNames.CORRECT)
  }

  fun `test submission added after code task check with periodically check`() {
    doTestSubmissionsAddedAfterCodeTaskCheck()
  }

  fun `test submission added after code task check with web sockets`() {
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

    doTestSubmissionsAddedAfterCodeTaskCheck()
  }

  private fun doTestSubmissionsAddedAfterCodeTaskCheck() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        codeTask("task1", stepId = 4) {
          taskFile("Task.txt", "fun foo() {}")
        }
      }
    } as HyperskillCourse

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Wrong solution" }

    doTestSubmissionAddedAfterTaskCheck(4, EduNames.WRONG)
  }

  @Language("JSON")
  private val submissions = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "7565797",
          "id": "7565000",
          "status": "wrong",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        },
        {
          "attempt": "7565798",
          "id": "7565001",
          "status": "wrong",
          "step": 2,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        },
        {
          "attempt": "7565799",
          "id": "7565002",
          "status": "correct",
          "step": 2,
          "time": "2020-04-29T11:45:20.422Z",
          "user": 6242591
        }
      ]
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
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
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
          "data": $submissions
        }
      }
    }    
   """
}