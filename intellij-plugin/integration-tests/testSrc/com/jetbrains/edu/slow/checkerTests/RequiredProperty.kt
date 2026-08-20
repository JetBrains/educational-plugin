package com.jetbrains.edu.slow.checkerTests

/**
 * Declares a property which has to be provided to run the annotated test case (or all test cases in the annotated class)
 * and which is passed as is into the IDE process launched by the test.
 *
 * @see CourseValidationTestBase
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class RequiredProperty(val property: String)
