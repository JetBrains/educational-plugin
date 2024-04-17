package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.WRONG
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.intellij.lang.annotations.Language
import org.junit.Test

class HyperskillSubmissionsTest : SubmissionsTestBase() {

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
    configureResponses()
  }

  override fun tearDown() {
    try {
      logOutFakeHyperskillUser()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  private fun configureResponses() {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
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

  @Test
  fun `test edu task in framework lesson submissions loaded`() {
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

  @Test
  fun `test edu problem submissions loaded`() {
    val stepId = 5
    hyperskillCourseWithFiles {
      section("Topics") {
        lesson("Topic name") {
          eduTask("Problem name", stepId = stepId) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests1.kt", "fun tests1() {}")
            taskFile("resources/application.properties")
            taskFile("task.html")
            taskFile("build.gradle")
          }
        }
      }
      additionalFile("build.gradle")
    }

    doTestSubmissionsLoaded(setOf(stepId), mapOf(stepId to 1))
  }

  @Test
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

  @Test
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

  @Test
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
    doTestSubmissionAddedAfterTaskCheck(3, CORRECT)
  }

  @Test
  fun `test submission added after code task check with periodically check`() {
    mockConnector.withWebSocketListener(object : WebSocketListener() {
      override fun onMessage(webSocket: WebSocket, text: String) {
        webSocket.cancel() // close violently
      }
    })

    doTestSubmissionsAddedAfterCodeTaskCheck()
  }

  @Test
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

    doTestSubmissionAddedAfterTaskCheck(4, WRONG)
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
        },
        {
          "attempt": "7565800",
          "id": "7565003",
          "status": "correct",
          "step": 5,
          "time": "2020-04-29T11:46:20.422Z",
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
      "push": {
        "channel": "submission#6242591-0",
        "pub": {
          "data": $submissions
        }
      }
    }    
   """
}