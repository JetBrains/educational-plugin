package com.jetbrains.edu.aiHints.core

fun <R> assertNoErrorsLogged(runnable: () -> R): R = runnable()