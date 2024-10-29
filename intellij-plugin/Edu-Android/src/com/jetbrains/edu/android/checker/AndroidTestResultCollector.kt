package com.jetbrains.edu.android.checker

import com.android.ddmlib.IDevice
import com.android.tools.idea.execution.common.AndroidExecutionTarget
import com.android.tools.idea.execution.common.processhandler.AndroidProcessHandler
import com.android.tools.idea.testartifacts.instrumented.testsuite.api.AndroidTestResultsTreeNode
import com.android.tools.idea.testartifacts.instrumented.testsuite.model.AndroidDevice
import com.android.tools.idea.testartifacts.instrumented.testsuite.model.AndroidDeviceType
import com.android.tools.idea.testartifacts.instrumented.testsuite.model.AndroidTestCaseResult
import com.android.tools.idea.testartifacts.instrumented.testsuite.view.AndroidTestResultsTableView
import com.android.tools.idea.testartifacts.instrumented.testsuite.view.AndroidTestSuiteView
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.jvm.gradle.checker.GradleTestResultCollector
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.checker.tests.TestResultGroup
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.PresentableStatus

/**
 * Collects events about both unit and instrumentation Android tests.
 *
 * Unfortunately, Android plugin doesn't provide a proper API to get test events for instrumentation tests.
 * So, as a workaround, this collector takes the corresponding custom test view and collects test results from it
 * using [com.intellij.execution.ExecutionListener] API
 */
class AndroidTestResultCollector : TestResultCollector() {

  private val unitTestResultCollector = GradleTestResultCollector()
  private val executionListener = AndroidProcessExecutionListener()

  override fun doStartCollecting(connection: MessageBusConnection) {
    unitTestResultCollector.startCollecting(connection)
    connection.subscribe(ExecutionManager.EXECUTION_TOPIC, executionListener)
  }

  override fun doCollectTestResults(): List<TestResultGroup> {
    val unitTestResults = unitTestResultCollector.collectTestResults()

    val (resultView, device) = executionListener.result ?: return unitTestResults
    val node = invokeAndWaitIfNeeded { resultView.rootResultsNode }

    return unitTestResults + listOfNotNull(node.toTestGroup(device.toAndroidDevice()))
  }

  private fun AndroidTestResultsTreeNode.toTestGroup(device: AndroidDevice): TestResultGroup? {

    val leafNodes = mutableListOf<AndroidTestResultsTreeNode>()

    fun AndroidTestResultsTreeNode.collectLeafNodes() {
      val children = childResults.toList()
      if (children.isEmpty()) {
        leafNodes += this
      }
      else {
        for (child in children) {
          child.collectLeafNodes()
        }
      }
    }

    collectLeafNodes()

    if (leafNodes.isEmpty()) return null

    val testResults = leafNodes.map {
      EduTestInfo(it.results.methodName, it.results.getTestResultSummary().toTestStatus(), it.results.getErrorStackTrace(device))
    }
    return TestResultGroup(testResults)
  }

  @Suppress("DEPRECATION")
  private fun IDevice.toAndroidDevice(): AndroidDevice {
    return AndroidDevice(
      serialNumber,
      avdName.orEmpty(),
      avdName.orEmpty(),
      if (isEmulator) AndroidDeviceType.LOCAL_EMULATOR else AndroidDeviceType.LOCAL_PHYSICAL_DEVICE,
      version
    )
  }

  private fun AndroidTestCaseResult.toTestStatus(): PresentableStatus {
    return when (this) {
      AndroidTestCaseResult.FAILED -> PresentableStatus.FAILED
      AndroidTestCaseResult.SKIPPED -> PresentableStatus.SKIPPED
      AndroidTestCaseResult.PASSED -> PresentableStatus.COMPLETED
      AndroidTestCaseResult.IN_PROGRESS -> PresentableStatus.RUNNING
      AndroidTestCaseResult.CANCELLED -> PresentableStatus.TERMINATED
      AndroidTestCaseResult.SCHEDULED -> PresentableStatus.NOT_RUN
    }
  }

  private inner class AndroidProcessExecutionListener : ExecutionListener {

    @Volatile
    var result: InstrumentationTestResult? = null

    override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
      if (executorId == DefaultRunExecutor.EXECUTOR_ID && handler is AndroidProcessHandler) {
        val resultView = (env.contentToReuse?.executionConsole as? AndroidTestSuiteView)?.myResultsTableView ?: return
        val device = (env.executionTarget as? AndroidExecutionTarget)?.runningDevices?.firstOrNull() ?: return
        result = InstrumentationTestResult(resultView, device)
      }
    }
  }

  private data class InstrumentationTestResult(
    val resultView: AndroidTestResultsTableView,
    val device: IDevice
  )
}
