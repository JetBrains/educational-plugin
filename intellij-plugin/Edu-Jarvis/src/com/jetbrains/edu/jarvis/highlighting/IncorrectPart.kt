package com.jetbrains.edu.jarvis.highlighting

/**
 * Represents the incorrect part to be highlighted.
 * @property range the range of the highlighting
 * @property parametrizedError the type of error to display
 */
data class IncorrectPart(val range: IntRange, val parametrizedError: AnnotatorParametrizedError)
