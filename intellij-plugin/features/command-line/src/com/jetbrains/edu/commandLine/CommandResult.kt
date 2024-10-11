package com.jetbrains.edu.commandLine

sealed class CommandResult {
  abstract val exitCode: Int

  data object Ok : CommandResult() {
    override val exitCode: Int = 0
  }

  data class Error(val message: String, val throwable: Throwable? = null) : CommandResult() {
    override val exitCode: Int = 1
  }
}
