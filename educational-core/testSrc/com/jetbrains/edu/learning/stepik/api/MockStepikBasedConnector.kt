package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.ResponseHandler

interface MockStepikBasedConnector {
  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockStepikBasedConnector
}