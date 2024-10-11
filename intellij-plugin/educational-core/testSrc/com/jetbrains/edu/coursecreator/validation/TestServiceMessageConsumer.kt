package com.jetbrains.edu.coursecreator.validation

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.ServiceMessageUtil
import jetbrains.buildServer.messages.serviceMessages.*
import kotlin.test.assertEquals

class TestServiceMessageConsumer : ServiceMessageConsumer {

  private val roots: MutableList<TestNode> = mutableListOf()

  private var currentTestSuites: MutableList<TestSuite> = mutableListOf()
  private var currentTestCase: TestCase? = null

  // This implementation assumes that all tests are executed sequentially
  override fun consume(message: ServiceMessageBuilder) {
    when (val parsedMessage = ServiceMessageUtil.parse(message.toString(), true)) {
      is TestSuiteStarted -> {
        currentTestSuites += TestSuite(parsedMessage.suiteName)
      }
      is TestSuiteFinished -> {
        val currentTestSuite = currentTestSuites.lastOrNull()
        if (currentTestSuite == null) error("`${parsedMessage.suiteName}` test suite didn't start")
        val currentTestCase = currentTestCase
        if (currentTestCase != null) error("`${currentTestCase.name}` test case is not finished yet")
        assertEquals(currentTestSuite.name, parsedMessage.suiteName)
        currentTestSuites.removeLast()
        val parentChildren = currentTestSuites.lastOrNull()?.children ?: roots
        parentChildren += currentTestSuite
      }
      is TestStarted -> {
        val currentTestCase = currentTestCase
        if (currentTestCase != null) error("`${currentTestCase.name}` test case is not finished yet")
        this.currentTestCase = TestCase(parsedMessage.testName)
      }
      is TestIgnored -> {
        val currentTestCase = currentTestCase
        if (currentTestCase == null) error("`${parsedMessage.testName}` test case didn't start")
        assertEquals(currentTestCase.name, parsedMessage.testName)
        this.currentTestCase = TestCase(parsedMessage.testName, TestStatus.IGNORED)
      }
      is TestFailed -> {
        val currentTestCase = currentTestCase
        if (currentTestCase == null) error("`${parsedMessage.testName}` test case didn't start")
        assertEquals(currentTestCase.name, parsedMessage.testName)
        this.currentTestCase = TestCase(parsedMessage.testName, TestStatus.FAILED)
      }
      is TestFinished -> {
        val currentTestCase = currentTestCase
        if (currentTestCase == null) error("`${parsedMessage.testName}` test case didn't start")
        assertEquals(currentTestCase.name, parsedMessage.testName)
        val parentChildren = currentTestSuites.lastOrNull()?.children ?: roots
        parentChildren += currentTestCase
        this.currentTestCase = null
      }
      else -> error("Unexpected test event: ")
    }
  }

  fun assertTestTreeEquals(expected: String) {
    val builder = StringBuilder()
    roots.print(0, builder)
    assertEquals(expected.trimIndent().trimEnd(), builder.toString().trimEnd())
  }

  private fun List<TestNode>.print(level: Int, out: StringBuilder) {
    for (node in this) {
      when (node) {
        is TestCase -> {
          out.append("  ".repeat(level))
          out.appendLine("- ${node.name}: ${node.status}")
        }
        is TestSuite -> {
          out.append("  ".repeat(level))
          out.appendLine("- ${node.name}:")
          node.children.print(level + 1, out)
        }
      }
    }
  }
}

private sealed interface TestNode {
  val name: String
}

private class TestSuite(override val name: String) : TestNode {
  val children: MutableList<TestNode> = mutableListOf()
}

private class TestCase(override val name: String, val status: TestStatus = TestStatus.SUCCESS) : TestNode

private enum class TestStatus {
  SUCCESS,
  IGNORED,
  FAILED;

  override fun toString(): String = name.lowercase()
}
