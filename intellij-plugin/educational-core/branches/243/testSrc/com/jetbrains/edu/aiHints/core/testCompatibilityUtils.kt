package com.jetbrains.edu.aiHints.core

import com.intellij.testFramework.rethrowLoggedErrorsIn

// BACKCOMPAT: 242. Inline it
@Suppress("DEPRECATION")
fun <R> assertNoErrorsLogged(runnable: () -> R): R {
  var result: R? = null
  rethrowLoggedErrorsIn {
    result = runnable()
  }
  @Suppress("UNCHECKED_CAST")
  return result as R
}