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
    fun getTestsOutput(processOutput: ProcessOutput, isAdaptive: Boolean): TestsOutput {
        //gradle prints compilation failures to error stream
        if (CheckUtils.hasCompilationErrors(processOutput)) {
            LOG.info(processOutput.stderr)
            return TestsOutput(false, COMPILATION_FAILED_MESSAGE)
        }

        val lines = processOutput.stdoutLines.filter { it.startsWith(STUDY_PREFIX) }
        var congratulations = CONGRATULATIONS
        for ((i, line) in lines.withIndex()) {
            if (line.contains(TEST_OK)) {
                continue
            }

            if (line.contains(CONGRATS_MESSAGE)) {
                congratulations = line.substringAfter(CONGRATS_MESSAGE)
            }

            if (line.contains(TEST_FAILED)) {
                if (!isAdaptive) {
                    return TestsOutput(false, line.substringAfter(TEST_FAILED))
                }
                else {
                    val builder = StringBuilder(line.substringAfter(TEST_FAILED) + "\n")
                    for (j in i + 1 until lines.size) {
                        val failedTextLine = lines[j]
                        if (!failedTextLine.contains(STUDY_PREFIX) || !failedTextLine.contains(CONGRATS_MESSAGE)) {
                            builder.append(failedTextLine)
                            builder.append("\n")
                        }
                        else {
                            break
                        }
                    }
                    return TestsOutput(false, builder.toString())
                }
            }
        }

        return TestsOutput(true, congratulations)
    }
}
