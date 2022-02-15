package com.jetbrains.edu.rust.checker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import org.rust.openapiext.execute

internal fun GeneralCommandLine.executeCargoCommandLine(disposable: Disposable, input: String? = null): ProcessOutput {
  return execute(disposable, stdIn = input?.toByteArray())
}
