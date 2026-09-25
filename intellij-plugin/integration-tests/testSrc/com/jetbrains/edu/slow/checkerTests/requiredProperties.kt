package com.jetbrains.edu.slow.checkerTests

// Properties to be used with `RequiredProperty`.
// They are collected in a single place since the same property may be required by tests of several technologies,
// e.g. `JDK_PROPERTY` is necessary for all JVM-based tests.

/**
 * Path to a JDK used as the project SDK
 */
const val JDK_PROPERTY = "project.jdk"

/**
 * Path to a Python interpreter used as the project SDK
 */
const val PYTHON_INTERPRETER_PROPERTY = "project.python.interpreter"
