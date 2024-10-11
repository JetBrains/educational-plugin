package com.jetbrains.edu.coursecreator.validation

import com.intellij.execution.testframework.sm.ServiceMessageBuilder

private const val MESSAGE = "message"
private const val DETAILS = "details"

/**
 * Returns `true` if all tests in suits are successful, `false` otherwise
 */
suspend fun withTestSuiteBuilder(serviceMessageConsumer: ServiceMessageConsumer, build: suspend TestSuiteBuilder.() -> Unit): Boolean {
  val suiteBuilder = TestSuiteBuilder(serviceMessageConsumer)
  suiteBuilder.build()
  return suiteBuilder.result
}

@DslMarker
annotation class TestEventDsl

@TestEventDsl
class TestSuiteBuilder(private val serviceMessageConsumer: ServiceMessageConsumer) {
  var result: Boolean = true

  suspend fun testSuite(name: String, build: suspend TestSuiteBuilder.() -> Unit) {
    serviceMessageConsumer.consume(ServiceMessageBuilder.testSuiteStarted(name))
    try {
      val nestedResult = withTestSuiteBuilder(serviceMessageConsumer, build)
      result = result || nestedResult
    }
    finally {
      serviceMessageConsumer.consume(ServiceMessageBuilder.testSuiteFinished(name))
    }
  }

  /**
   * If a test fails, [build] is responsible for emitting additional test messages.
   *
   * @see [TestCaseBuilder.testFailed]
   * @see [TestCaseBuilder.testIgnored]
   */
  suspend fun testCase(name: String, build: suspend TestCaseBuilder.() -> Unit) {
    val builder = TestCaseBuilder(name, serviceMessageConsumer)

    serviceMessageConsumer.consume(ServiceMessageBuilder.testStarted(name))
    try {
      builder.build()
      result = result || builder.result
    }
    finally {
      serviceMessageConsumer.consume(ServiceMessageBuilder.testFinished(name))
    }
  }
}

@TestEventDsl
class TestCaseBuilder(private val name: String, private val serviceMessageConsumer: ServiceMessageConsumer) {

  var result = true

  fun testFailed(message: String, details: String = "") {
    val sm = ServiceMessageBuilder.testFailed(name)
      .addAttribute(MESSAGE, message)
      .addAttribute(DETAILS, details)
    serviceMessageConsumer.consume(sm)
    result = false
  }

  fun testIgnored(message: String) {
    val sm = ServiceMessageBuilder.testIgnored(name)
      .addAttribute(MESSAGE, message)
    serviceMessageConsumer.consume(sm)
    result = false
  }
}
