package com.jetbrains.edu.jarvis

import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError

/**
 * Returns an [AnnotatorParametrizedError] associated with the relevant context.
 */
interface ErrorProcessor {

  fun processNamedFunction(): AnnotatorParametrizedError

  fun processNamedVariable(): AnnotatorParametrizedError
}
