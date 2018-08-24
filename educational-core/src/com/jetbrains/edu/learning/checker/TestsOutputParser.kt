package com.jetbrains.edu.learning.checker

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.STUDY_PREFIX


object TestsOutputParser {
  const val TEST_OK = "test OK"
  const val TEST_FAILED = "FAILED + "
  const val CONGRATS_MESSAGE = "CONGRATS_MESSAGE "
  const val CONGRATULATIONS = "Congratulations!"

  private val LOG = Logger.getInstance(TestsOutputParser.javaClass)

  class TestsOutput(val isSuccess: Boolean, val message: String)

  @JvmStatic
  fun getTestsOutput(processOutput: ProcessOutput): TestsOutput {
    //gradle prints compilation failures to error stream
    if (CheckUtils.hasCompilationErrors(processOutput)) {
      LOG.info(processOutput.stderr)
      return TestsOutput(false, COMPILATION_FAILED_MESSAGE)
    }

    val lines = processOutput.stdoutLines.filter { it.startsWith(STUDY_PREFIX) }
    var congratulations = CONGRATULATIONS
    for (line in lines) {
      if (line.contains(TEST_OK)) {
        continue
      }

      if (line.contains(CONGRATS_MESSAGE)) {
        congratulations = line.substringAfter(CONGRATS_MESSAGE)
      }

      if (line.contains(TEST_FAILED)) {
        return TestsOutput(false, line.substringAfter(TEST_FAILED))
      }
    }

    return TestsOutput(true, congratulations)
  }
}
