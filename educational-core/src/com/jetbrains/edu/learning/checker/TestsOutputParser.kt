package com.jetbrains.edu.learning.checker

import com.intellij.execution.process.ProcessOutput
import com.jetbrains.edu.learning.checker.CheckUtils.*


object TestsOutputParser {
    private const val TEST_OK = "test OK"
    private const val TEST_FAILED = "FAILED + "
    private const val CONGRATS_MESSAGE = "CONGRATS_MESSAGE "
    const val CONGRATULATIONS = "Congratulations!"

    class TestsOutput(val isSuccess: Boolean, val message: String)

    @JvmStatic
    fun getTestsOutput(processOutput: ProcessOutput, isAdaptive: Boolean): TestsOutput {
        //gradle prints compilation failures to error stream
        if (hasCompilationErrors(processOutput)) {
            return TestsOutput(false, COMPILATION_FAILED_MESSAGE)
        }
        var congratulations = CONGRATULATIONS
        val lines = processOutput.stdoutLines
        for ((i, line) in lines.withIndex()) {
            if (line.startsWith(STUDY_PREFIX)) {
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
        }

        return TestsOutput(true, congratulations)
    }

    private fun hasCompilationErrors(processOutput: ProcessOutput) = processOutput.stderrLines.any { it.contains(BUILD_FAILED) }
}
