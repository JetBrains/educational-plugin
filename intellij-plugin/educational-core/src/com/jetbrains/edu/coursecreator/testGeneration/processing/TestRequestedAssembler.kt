package com.jetbrains.edu.coursecreator.testGeneration.processing

import com.intellij.util.io.HttpRequests
import org.jetbrains.research.testspark.core.test.TestsAssembler

abstract class TestRequestedAssembler: TestsAssembler() {
  abstract fun consume(httpRequest: HttpRequests.Request)
}
