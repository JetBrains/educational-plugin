package com.jetbrains.edu.rules

/**
 * Sets custom value for a registry key during test execution.
 *
 * You should use string representation of the necessary value
 *
 * @see [CustomValuesRule]
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class WithRegistryValue(val key: String, val value: String)
