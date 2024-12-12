package com.jetbrains.edu.rust.checker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import org.rust.openapiext.RsProcessExecutionException
import org.rust.openapiext.execute
import org.rust.stdext.RsResult

// BACKCOMPAT: 2024.1. Move into `utils.kt` in `com.jetbrains.edu.rust.checker` package
internal fun GeneralCommandLine.executeCargoCommandLine(
  // BACKCOMPAT: 2024.1. Drop this parameter
  @Suppress("UNUSED_PARAMETER") disposable: Disposable,
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
