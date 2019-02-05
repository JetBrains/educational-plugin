package com.jetbrains.edu.learning

sealed class Result<out T, out E>
data class Ok<out T>(val value: T) : Result<T, Nothing>()
open class Err<out E>(val error: E) : Result<Nothing, E>()
