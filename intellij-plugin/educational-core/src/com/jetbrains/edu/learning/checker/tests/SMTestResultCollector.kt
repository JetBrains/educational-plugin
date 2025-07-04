package com.jetbrains.edu.learning.checker.tests

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.checker.CheckUtils.fillWithIncorrect
import com.jetbrains.edu.learning.checker.CheckUtils.removeAttributes
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle

/**
 * Collects test results using [SMTRunnerEventsListener.TEST_STATUS] topic.
 *
 * Override [getErrorMessage] and [getComparisonErrorMessage] to modify test messages
 */
open class SMTestResultCollector : TestResultCollector() {

  private val testListener = TestEventListener()

  final override fun doStartCollecting(connection: MessageBusConnection) {
    connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, testListener)
  }

  final override fun doCollectTestResults(): List<TestResultGroup> {
    return testListener.testRoots.map {
      TestResultGroup(getEduTestInfo(children = it.children))
    }
  }

  /**
   * Returns message for test error that will be shown to a user in Check Result panel
   */
  @Suppress("UnstableApiUsage")
  @NlsSafe
  protected open fun getErrorMessage(node: SMTestProxy): String = node.errorMessage ?: EduCoreBundle.message("error.execution.failed")

  /**
   * Returns message for comparison error that will be shown to a user in Check Result panel
   */
  protected open fun getComparisonErrorMessage(node: SMTestProxy): String = getErrorMessage(node)

  private fun getEduTestInfo(paths: MutableList<String> = mutableListOf(), children: List<SMTestProxy>): List<EduTestInfo> {
    val result = mutableListOf<EduTestInfo>()
    children.forEach {
      paths.add(it.presentableName)
      if (it.isLeaf) {
        // Submission Service stores a test name with a maximum length of 255 characters.
        // The number 245 is chosen to allow space for [1], [2], etc., for possible duplicate test names.
        var testName = paths.joinToString(":").take(245)
        val testCount = result.count { test -> test.name == testName }
        if (testCount > 0) {
          testName = "$testName[$testCount]"
        }
        result.add(createEduTestInfo(it, testName))
      }
      else {
        result.addAll(getEduTestInfo(paths, it.children))
      }
      paths.removeLast()
    }
    return result
  }

  /**
   * Gathers test results info from given [node] and creates [EduTestInfo]
   */
  protected open fun createEduTestInfo(node: SMTestProxy, name: String): EduTestInfo {
    val diff = node.diffViewerProvider
    val message = if (diff != null) getComparisonErrorMessage(node) else getErrorMessage(node)

    return node.toEduTestInfo(name, message, diff?.let { CheckResultDiff(diff.left, diff.right, diff.diffTitle) })
  }

  /**
   * Creates [EduTestInfo] from given data.
   *
   * It's not supposed to be overridden and created only to reduce code duplication if you need to override [createEduTestInfo]
   */
  protected fun SMTestProxy.toEduTestInfo(name: String, message: String, diff: CheckResultDiff?): EduTestInfo {
    return EduTestInfo(
      name = name,
      status = magnitudeInfo.value,
      message = removeAttributes(fillWithIncorrect(message)),
      details = stacktrace,
      isFinishedSuccessfully = finishedSuccessfully(),
      checkResultDiff = diff
    )
  }

  private fun SMTestProxy.finishedSuccessfully(): Boolean {
    return !hasErrors() && (isPassed || isIgnored)
  }

  companion object {
    @JvmStatic
    protected fun extractComparisonErrorMessage(node: SMTestProxy): String {
      val errorMessage = node.errorMessage.orEmpty()
      val index = StringUtil.indexOfIgnoreCase(errorMessage, "expected:", 0)
      return if (index != -1) errorMessage.substring(0, index).trim() else errorMessage
    }
  }

  private inner class TestEventListener : SMTRunnerEventsAdapter() {
    val testRoots = mutableListOf<SMTestProxy.SMRootTestProxy>()

    // We have to collect test roots in `onTestingStarted`
    // because some test framework integrations (Gradle)
    // have custom components in implementation that don't provide test events
    // except `onTestingStarted`
    override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
      testRoots += testsRoot
    }
  }
}
