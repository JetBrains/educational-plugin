package com.jetbrains.edu.learning

sealed class Result<out T, out E>
data class Ok<out T>(val value: T) : Result<T, Nothing>()
open class Err<out E>(val error: E) : Result<Nothing, E>()

inline fun <T, S, E> Result<T, E>.map(func: (T) -> S): Result<S, E> {
  return when (this) {
    is Err -> this
    is Ok -> Ok(func(value))
  }
}

inline fun <T, S, E> Result<T, E>.flatMap(func: (T) -> Result<S, E>): Result<S, E> {
  return when (this) {
    is Err -> this
    is Ok -> func(value)
  }
}

inline fun <T, E> Result<T, E>.onError(action: (E) -> T): T {
  return when (this) {
    is Ok -> value
    is Err -> action(error)
  }
}