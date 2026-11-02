package com.jetbrains.edu.slow.checkerTests

/**
 * Declares a Marketplace plugin which has to be installed into the IDE launched by the annotated test case
 * (or by all test cases in the annotated class and its subclasses).
 * The latest plugin version compatible with the tested IDE build is installed.
 *
 * @see CourseValidationTestBase
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class RequiredPlugin(val pluginId: String)
