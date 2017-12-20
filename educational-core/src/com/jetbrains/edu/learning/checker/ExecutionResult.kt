package com.jetbrains.edu.learning.checker

sealed class ExecutionResult<out T, out E>
data class Ok<out T>(val value: T) : ExecutionResult<T, Nothing>()
data class Err<out E>(val error: E) : ExecutionResult<Nothing, E>()
