package com.jetbrains.edu.rules

/**
 * Sets custom value for a experimental feature during test execution
 *
 * @see [CustomValuesRule]
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class WithExperimentalFeature(val id: String, val value: Boolean)