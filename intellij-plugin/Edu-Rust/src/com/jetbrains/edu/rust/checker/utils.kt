package com.jetbrains.edu.rust.checker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import org.rust.openapiext.RsProcessExecutionException
import org.rust.openapiext.execute
import org.rust.stdext.RsResult

const val COMPILATION_ERROR_MESSAGE = "error: could not compile"

internal fun GeneralCommandLine.executeCargoCommandLine(
  input: String? = null
): ProcessOutput {
  return when (val result = execute(stdIn = input?.toByteArray())) {
    is RsResult.Ok -> result.ok
    is RsResult.Err -> {
      when (val err = result.err) {
        // `err.cause` is ExecutionException by `RsProcessExecutionException.Start` constructor
        // but it's not declared in types so let's just add `?: err` to make it compile
        is RsProcessExecutionException.Start -> throw err.cause ?: err
        is RsProcessExecutionException.ProcessAborted -> err.output
      }
    }
  }
}