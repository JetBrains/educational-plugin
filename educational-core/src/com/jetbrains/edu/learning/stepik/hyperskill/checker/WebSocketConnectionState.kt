package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.stepik.api.SubmissionsList
import com.jetbrains.edu.learning.stepik.checker.StepikBaseCheckConnector.Companion.toCheckResult
import com.jetbrains.edu.learning.stepik.checker.StepikBaseSubmitConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.EVALUATION_STATUS
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import okhttp3.WebSocket

/**
 * Communication protocol with Hyperskill WS is the following:
 *
 * 1. Retrieve token needed to authorize via API
 * 2. Send token to server when connection opens: {"params":{"token":"<actual token>"},"id":1}
 * 3. Receive OK message: {"id":1,"result":{"client":"<clientId>","version":"2.3.1","expires":true,"ttl":899}}
 * 4. Send message to subscribe to submission events: {"method":1,"params":{"channel":"submission#<userId>-0"},"id":2}
 * 5. Receive OK message: {"id":2,"result":{}}
 * 6. Start receiving messages with submission events: {"result":{"channel":"submission#6242591-0","data":{"data": <submissionsData>}}}
 */
abstract class WebSocketConnectionState(protected val project: Project, protected val task: CodeTask, val isTerminal: Boolean = false) {
  abstract fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState

  open fun getResult(): Result<CheckResult, SubmissionError> {
    return Err(SubmissionError.NoSubmission("Submission result check via web sockets failed"))
  }
}

class InitialState(project: Project, task: CodeTask, private val token: String) : WebSocketConnectionState(project, task) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    webSocket.send(OpenMessage(token))
    return WaitingForConnectionState(project, task)
  }
}

private class WaitingForConnectionState(project: Project, task: CodeTask) : WebSocketConnectionState(project, task) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    webSocket.send(SubscribeToSubmissionsMessage(HyperskillSettings.INSTANCE.account!!.userInfo.id))
    return WaitingForSubscriptionState(project, task)
  }
}


private class WaitingForSubscriptionState(project: Project, task: CodeTask) : WebSocketConnectionState(project, task) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    return when (val result: Result<Submission, String> = StepikBaseSubmitConnector.submitCodeTask(project, task)) {
      is Ok -> ReceivingSubmissionsState(project, task, result.value)
      is Err -> ErrorState(project, task)
    }
  }
}

private class ReceivingSubmissionsState(project: Project, task: CodeTask, val submission: Submission) : WebSocketConnectionState(project,
                                                                                                                                 task) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    val objectMapper = HyperskillConnector.getInstance().objectMapper
    val dataKey = "data"
    val data = objectMapper.readTree(message).get("result").get(dataKey)?.get(dataKey) ?: return this
    for (receivedSubmission in objectMapper.treeToValue(data, SubmissionsList::class.java).submissions) {
      if (receivedSubmission.status == EVALUATION_STATUS) continue
      if (submission.id == receivedSubmission.id) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.id, receivedSubmission)
        return SubmissionReceivedState(project, task, receivedSubmission)
      }
    }
    return this
  }

  override fun getResult(): Result<CheckResult, SubmissionError> {
    return Err(SubmissionError.WithSubmission(submission, "No check result received"))
  }
}

private class SubmissionReceivedState(project: Project, task: CodeTask, private val submission: Submission) : WebSocketConnectionState(
  project, task, true) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    return this
  }

  override fun getResult(): Result<CheckResult, SubmissionError> {
    return Ok(submission.toCheckResult())
  }
}

private class ErrorState(project: Project, task: CodeTask) : WebSocketConnectionState(project, task, true) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    return this
  }
}

private fun WebSocket.send(message: WebSocketMessage) {
  send(HyperskillConnector.getInstance().objectMapper.writeValueAsString(message))
}

private open class WebSocketMessage(@field:JsonProperty("id") val id: Int)

private class OpenMessage(token: String) : WebSocketMessage(1) {
  @JsonProperty("params")
  val params = mapOf("token" to token)
}

private class SubscribeToSubmissionsMessage(userId: Int) : WebSocketMessage(2) {
  @JsonProperty("method")
  val method = 1

  @JsonProperty("params")
  val params = mapOf("channel" to "submission#$userId-0")
}