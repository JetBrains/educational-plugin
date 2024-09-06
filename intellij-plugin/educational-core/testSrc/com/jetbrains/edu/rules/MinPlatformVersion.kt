package com.jetbrains.edu.rules

/**
 * Specify minimal platform version necessary for test execution
 *
 * @see [ConditionalExecutionRule]
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class MinPlatformVersion(val version: String)
