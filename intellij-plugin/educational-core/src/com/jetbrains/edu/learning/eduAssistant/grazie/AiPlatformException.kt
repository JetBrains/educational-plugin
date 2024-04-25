package com.jetbrains.edu.learning.eduAssistant.grazie

import ai.grazie.model.cloud.exceptions.HTTPStatusException
import ai.grazie.model.cloud.sse.continuous.ContinuousSSEExceptionEvent
import com.jetbrains.edu.learning.eduAssistant.core.AssistantError
import com.jetbrains.edu.learning.eduAssistant.log.Loggers
import java.net.SocketException
import java.nio.channels.UnresolvedAddressException

class AiPlatformException(
  val assistantError: AssistantError,
  cause: Throwable
) : Throwable("${assistantError.errorMessage} (${assistantError.name})", cause) {
  constructor(cause: Throwable) : this(AssistantError.UnknownError, cause)

  init {
    Loggers.eduAssistantLogger.error("Ai platform error occurs: ${assistantError.errorMessage}", cause)
  }
}

fun Throwable.toAiPlatformException(): AiPlatformException = when (this) {
  is SocketException,
  is ContinuousSSEExceptionEvent.Timeout,
  is UnresolvedAddressException -> AiPlatformException(AssistantError.NetworkError, this)
  is HTTPStatusException.Unauthorized -> AiPlatformException(AssistantError.AuthError, this)
  is HTTPStatusException.TooManyRequests -> AiPlatformException(AssistantError.TooManyRequests, this)
  else -> AiPlatformException(this)
}